# Private appearance backup

The app keeps a private backup of cosmetic preferences and account files in the existing Cloudflare D1 database. It restores a clean reinstall after authenticating the existing TikTok UID, then uploads later changes in the background. The backup is separate from the public profile-style sharing toggle.

The worker stores only a digest of a device recovery secret. The secret is derived from Android's app-scoped `ANDROID_ID`, the TikTok UID, and this build's pinned public-key identifier; the server never receives the Android ID itself. A device can reclaim its existing owner token after reinstall. A public username by itself cannot claim an account or authorize badge changes. A new device still needs the existing TikTok bio proof.

Migration `0009-appearance-backups.sql` adds the private snapshot, chunked file store, staged manifest, and trusted-device digest tables. D1 export should be kept before applying future schema changes.
