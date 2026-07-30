# Release checklist

Complete each item against intended release commit. Do not infer completion from this file.

- [ ] Working tree reviewed; release source committed intentionally
- [ ] Debug and release unit tests pass
- [ ] Debug and release lint pass; warnings reviewed
- [ ] Instrumentation suite passes on target device/emulator
- [ ] Room version, exported schema, and identity hash unchanged or deliberately migrated
- [ ] Production catalogue validates; every guided asset opens
- [ ] No test fixture, test audio, placeholder content, or unapproved media in production
- [ ] Code and media licenses reviewed; asset records complete
- [ ] Privacy behavior and hosted privacy-policy URL reviewed
- [ ] Version code/name, application ID, app label, launcher icon, splash, and store metadata approved
- [ ] Secure release signing configured outside repository
- [ ] Minified/resource-shrunk APK and AAB build; R8 warnings and mapping handling reviewed
- [ ] Installable signed release smoke-tested; merged release manifest is not debuggable
- [ ] Effective permissions and exported components reviewed
- [ ] Airplane-mode matrix passes
- [ ] TalkBack, font scale 1.0/1.3/2.0, enlarged display, portrait, and landscape pass
- [ ] Light/dark, reduced-motion, gesture-navigation, and system-bar review passes
- [ ] Reminder grant, denial, external block, time change, disable, and single-alarm cases pass
- [ ] Reminder reboot/time-zone rescheduling passes
- [ ] Media3 background, screen-off, focus, route-change, notification, and lock-screen tests pass with approved audio
- [ ] Store listing, data-safety/privacy, content rating, accessibility, and policy tasks complete
- [ ] Tagging, signing, checksum, and artifact publication performed manually
