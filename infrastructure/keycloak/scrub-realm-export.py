#!/usr/bin/env python3
"""
Scrub 1 realm export tho (da chay voi --users realm_file) thanh file an toan de commit:

  - GIU LAI service-account users (service-account-*) + role mappings cua chung
    -> KHONG mat quyen SA khi import lai (loi cu: --users skip lam mat ADMIN/realm-management).
  - BO cac user nguoi that (kem password hash) -> khong lo du lieu ca nhan.
  - Scrub client secret (Google IDP, healthcare-admin-cli) -> placeholder ${env.*}.
  - Dat brand MediBook + bat resetPasswordAllowed.

Dung:
  python scrub-realm-export.py <raw-export.json> [realm-config/healthcare-realm.json]
"""
import json, io, sys

src = sys.argv[1] if len(sys.argv) > 1 else "backups/export/healthcare-realm.json"
dst = sys.argv[2] if len(sys.argv) > 2 else "realm-config/healthcare-realm.json"

with io.open(src, encoding="utf-8") as f:
    data = json.load(f)

# 1) Brand
data["displayName"] = "MediBook"
data["displayNameHtml"] = "<span>Medi<b>Book</b></span>"
data["resetPasswordAllowed"] = True

# 2) Users: chi giu service-account-* (SA), bo user nguoi that + password hash
kept = []
for u in data.get("users", []) or []:
    if str(u.get("username", "")).startswith("service-account-"):
        u.pop("credentials", None)  # SA khong can credential
        kept.append(u)
data["users"] = kept

# 3) Scrub client secrets -> ${env.*}
for idp in data.get("identityProviders", []) or []:
    cfg = idp.get("config", {}) or {}
    if idp.get("alias") == "google" or "google" in str(idp.get("providerId", "")):
        if cfg.get("clientId"):
            cfg["clientId"] = "${env.GOOGLE_CLIENT_ID}"
        if cfg.get("clientSecret"):
            cfg["clientSecret"] = "${env.GOOGLE_CLIENT_SECRET}"

for c in data.get("clients", []) or []:
    if c.get("clientId") == "healthcare-admin-cli" and c.get("secret"):
        c["secret"] = "${env.KEYCLOAK_ADMIN_CLIENT_SECRET}"

# 4) Safety net: khong con GOCSPX / secret dang chuoi dai lot ra ngoai
raw = json.dumps(data, ensure_ascii=False, indent=2)
assert "GOCSPX-" not in raw, "Google secret con sot trong export!"

with io.open(dst, "w", encoding="utf-8") as f:
    f.write(raw + "\n")

print(f"scrubbed -> {dst}")
print(f"kept service-account users: {len(kept)}")
