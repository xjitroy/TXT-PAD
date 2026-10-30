# TXT privacy policy

Effective date: 30 October 2026

TXT is an offline text editor and document vault maintained in the public
[xjitroy/TXT-PAD repository](https://github.com/xjitroy/TXT-PAD).

## Information handling

TXT does not collect, transmit, sell or share your documents, passwords, device identifiers,
usage events or personal information with the developer. It has no account system,
analytics, advertising SDK, server connection, INTERNET permission or network-state permission.
All editing, DOCX conversion, password encryption, backup and restore run on your device.

Documents remain in the workspace folder you select with Android's file picker, or in
TXT's private app storage when you choose Private Vault. Trash is also stored in private
app storage. Plain files are readable to anyone with access to their storage location;
password protection must be chosen to encrypt a file. Private storage is Android app
isolation, and is not a guarantee against a compromised operating system or rooted device.

## Passwords and device authentication

Passwords derive AES-GCM encryption keys locally. TXT does not send them to the developer
and cannot recover forgotten file or backup passwords. While an encrypted document is open,
TXT can retain a RAM-only sealed credential to auto-save it. That token is cleared when the
document closes and is not written to disk. Password and key-buffer clearing is best effort;
Android, the keyboard and managed runtime can retain copies outside TXT's control.

Optional app lock uses Android's system biometric/device-credential prompt and AndroidKeyStore.
It is off by default. TXT does not receive fingerprint templates or your device PIN.

## User-selected system services

Android's document picker can list third-party cloud-storage providers. If you choose one,
that provider handles the selected document under its own policy. Choose a local folder
for entirely local storage. Android Autofill and your selected keyboard/password manager
are separate services governed by their own policies. TXT does not operate them.

## Backups, retention and deletion

TXT disables automatic Android app backup and device transfer for app data. User-created
.tpbak backups are password-encrypted and stored only where you choose. They include active
workspace and vault files; Trash and device-bound authentication keys are excluded.
Restore needed Trash files before exporting. Keep backups outside the phone before resetting it.

You control retention. Delete a Trash item permanently to remove TXT's recovery copy.
Uninstalling or clearing TXT's storage removes the private vault, Trash and settings.
Public workspace files and exported backups remain until you delete them using their storage
provider. TXT cannot promise physical secure erase on flash storage. There is no online account
to delete because TXT has no accounts.

## General audience

TXT is a general-purpose productivity tool. It has no social features, advertising or tracking.
It does not knowingly collect information from children or any other users.

## Contact and changes

For privacy questions, contact the maintainer using
[GitHub Issues](https://github.com/xjitroy/TXT-PAD/issues). Please do not post private documents,
passwords, signing keys or backup contents in public issues. Changes to this policy will be
published in this file with an updated effective date.
