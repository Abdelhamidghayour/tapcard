/**
 * TapCard Tier-2 public profile: server-rendered HTML with per-slug SEO +
 * Open Graph tags and a /vcard download endpoint.
 *
 * Deploy (Blaze plan): firebase deploy --only functions
 * Hosting rewrite (firebase.json) for /p/** should target "profilePage" to
 * enable this tier; the static default keeps Tier 1 working without it.
 */
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const esc = (s) =>
  String(s ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));

exports.profilePage = functions.https.onRequest(async (req, res) => {
  // Path forms: /p/{slug} and /p/{slug}/vcard
  const parts = req.path.split("/").filter(Boolean); // ["p", slug, ("vcard")]
  if (parts[0] !== "p" || !parts[1]) {
    res.status(404).send("Not found");
    return;
  }
  const slug = parts[1].toLowerCase();
  if (!/^[a-z0-9][a-z0-9-]{2,39}$/.test(slug)) {
    res.status(400).send("Invalid card link");
    return;
  }
  const wantsVcard = parts[2] === "vcard";

  let profile;
  try {
    const snap = await db.collection("publicProfiles").doc(slug).get();
    if (!snap.exists) {
      res.status(404).send("This business card link is not available.");
      return;
    }
    profile = snap.data();
  } catch (e) {
    functions.logger.error("profile fetch failed", { slug, error: e.message });
    res.status(500).send("Failed to load the card.");
    return;
  }

  if (wantsVcard) {
    res.set("Content-Type", "text/vcard; charset=utf-8");
    res.set("Content-Disposition",
      `attachment; filename="${slug}.vcf"`);
    res.status(200).send(buildVCard(profile, `https://tapcard.app/p/${slug}`));
    return;
  }

  const publicUrl = `https://tapcard.app/p/${slug}`;
  const role = [profile.jobTitle, profile.company].filter(Boolean).join(" · ");
  const origin = `https://${req.headers.host || "tapcard.app"}`;

  res.status(200).set("Content-Type", "text/html; charset=utf-8").send(`<!DOCTYPE html>
<html lang="en"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(profile.name)} — Digital Business Card</title>
<meta name="description" content="${esc(profile.name)}${role ? " — " + esc(role) : ""}. Tap to view and save the contact.">
<meta property="og:title" content="${esc(profile.name)} — Digital Business Card">
<meta property="og:description" content="${esc(role || "Shared with TapCard")}">
<meta property="og:type" content="profile">
<meta property="og:url" content="${esc(publicUrl)}">
${profile.profileImageUrl ? `<meta property="og:image" content="${esc(profile.profileImageUrl)}">` : ""}
<link rel="canonical" href="${esc(publicUrl)}">
<style>
body{font-family:-apple-system,"Segoe UI",Roboto,Inter,sans-serif;background:#F6F7FB;color:#0F172A;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px}
.card{width:100%;max-width:420px;background:#fff;border-radius:28px;box-shadow:0 24px 60px rgba(79,70,229,.18);overflow:hidden}
.hero{padding:36px 28px 28px;text-align:center;color:#fff;background:linear-gradient(135deg,#4F46E5,#7C3AED)}
.avatar{width:92px;height:92px;border-radius:50%;margin:0 auto 14px;object-fit:cover;border:3px solid rgba(255,255,255,.6);background:rgba(255,255,255,.25);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:30px}
h1{font-size:26px;letter-spacing:-.4px;margin:0}.role{opacity:.92;margin-top:6px;font-size:15px}
.body{padding:22px 20px 28px}.row{display:flex;gap:12px;padding:12px 6px;border-bottom:1px solid #EEF1F8;color:#0F172A;text-decoration:none}
.row:last-of-type{border-bottom:none}.row .label{font-size:12px;color:#64748B}.row .value{font-size:15px;word-break:break-all}
.actions{display:grid;gap:10px;margin-top:18px}.btn{display:block;text-align:center;padding:14px;border-radius:14px;font-weight:600;text-decoration:none;border:0;font-size:15px}
.btn.primary{background:linear-gradient(135deg,#4F46E5,#7C3AED);color:#fff}.btn.ghost{background:#EEF2FF;color:#4F46E5}
.muted{text-align:center;color:#64748B;font-size:13px;margin-top:16px}
</style></head>
<body><main class="card">
<div class="hero">
${profile.profileImageUrl
  ? `<img class="avatar" src="${esc(profile.profileImageUrl)}" alt="${esc(profile.name)}">`
  : `<div class="avatar">${esc(initialsOf(profile.name))}</div>`}
<h1>${esc(profile.name)}</h1>
${role ? `<p class="role">${esc(role)}</p>` : ""}
</div>
<div class="body">
${rowHtml("Phone", profile.phone, profile.phone ? `tel:${profile.phone.replace(/\s+/g, "")}` : null)}
${rowHtml("Email", profile.email, profile.email ? `mailto:${profile.email}` : null)}
${rowHtml("Website", profile.website, profile.website)}
${(profile.socials || []).map((s) => rowHtml(s.platform, s.url, s.url)).join("\n")}
<div class="actions">
<a class="btn primary" href="${esc(publicUrl)}/vcard">Save contact (vCard)</a>
<a class="btn ghost" href="${esc(publicUrl)}">Card link</a>
</div>
<p class="muted">Shared with TapCard</p>
</div></main></body></html>`);
});

function initialsOf(name) {
  return String(name || "").trim().split(/\s+/).slice(0, 2)
    .map((w) => (w[0] || "").toUpperCase()).join("");
}

function rowHtml(label, value, href) {
  if (!value) return "";
  return `<a class="row" href="${esc(href || "#")}"><div>
    <div class="label">${esc(label)}</div><div class="value">${esc(value)}</div></div></a>`;
}

function buildVCard(p, url) {
  const lines = ["BEGIN:VCARD", "VERSION:3.0", `FN:${p.name}`];
  const parts = String(p.name || "").trim().split(/\s+/);
  const last = parts.length > 1 ? parts[parts.length - 1] : parts[0];
  const first = parts.length > 1 ? parts[0] : "";
  lines.push(`N:${last};${first}`);
  if (p.jobTitle) lines.push(`TITLE:${p.jobTitle}`);
  if (p.company) lines.push(`ORG:${p.company}`);
  if (p.phone) lines.push(`TEL;TYPE=CELL:${p.phone}`);
  if (p.email) lines.push(`EMAIL;TYPE=INTERNET:${p.email}`);
  if (p.website) lines.push(`URL:${p.website}`);
  (p.socials || []).forEach((s) =>
    lines.push(`X-SOCIALPROFILE;TYPE=${String(s.platform).toLowerCase()}:${s.url}`));
  lines.push(`URL:${url}`, "END:VCARD");
  return lines.join("\r\n");
}
