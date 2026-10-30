# TXT: ব্যবহার ও APK তৈরির সহজ নিয়ম

সরাসরি APK: https://github.com/xjitroy/TXT-PAD/releases/download/v1.1.0/TXT.apk

Screenshot ও animation video: https://github.com/xjitroy/TXT-PAD/releases/tag/v1.1.0

এটি TXT 1.1.0-এর স্থায়ী production certificate দিয়ে signed build। Final signed-APK smoke ও publication-এর অবস্থা docs/RELEASE_STATUS.md-এ আছে।
নির্দিষ্ট build-এর পরীক্ষার ফল ও APK checksum
VERIFICATION.json ফাইলে আছে। Compilation, crypto tests ও lint পাস করেছে।
Android 8 ও Android 16-এ ২৬টি করে মোট ৫২টি emulator পরীক্ষা পাস করেছে। বাস্তব ফোনের সব মডেলে পরীক্ষা এখনও বাকি।

## ব্যবহার

1. প্রথমবার Choose workspace folder দিয়ে নিজের folder বাছুন বা তৈরি করুন।
2. New File চাপলে keyboard খুলবে। লেখা থামানোর ১.৫ সেকেন্ড পরে Auto-save হয়—ডিফল্টে অন।
   প্রথম লাইনের লেখা থেকে filename তৈরি হয়। উপরে Saving / Saved অবস্থা দেখা যায়।
3. Undo / Redo চাপলে পরিবর্তিত লেখার জায়গায় যাবে এবং পরিবর্তন নির্বাচন করে দেখাবে।
   Auto-save হলেও Undo / Redo history থাকে।
4. নিজের নামে সেভ করতে Tools ⋯ → Save As… চাপুন। index.html, script.php, notes.txt
   বা নিজের extension লিখে Choose location চাপুন। নতুন ফাইলে Auto-save চলবে; পুরোনোটি থাকবে।
5. Lock দিয়ে password দিন। এরপর ওই খোলা document Auto-save হবে। Document বন্ধ করে আবার
   খুললে password লাগবে। Password ভুলে গেলে TXT উদ্ধার করতে পারে না।
6. Tools থেকে font size/family, word wrap, line number ও private vault-এ move পাওয়া যায়।
   Keyboard খুললে শেষ লাইনের caret যেন দৃশ্যমান থাকে সেভাবে editor ছোট হয় ও scroll করে।
7. ফাইলের ⋮ মেনুতে Open, Rename, Move ও Move to Trash আছে। Trash থেকে Restore বা স্থায়ী
   Delete করা যায়। Trash backup-এ থাকে না—দরকারি ফাইল আগে restore করুন।
8. Light/Dark mode ফোনের System theme অনুযায়ী বদলায়। Android 12+ এ glass backdrop blur,
   moving light ও reflection আছে। পুরোনো Android-এ translucent fallback থাকে।
9. Settings → Fingerprint / screen lock ডিফল্টে অফ। সমর্থিত hardware ও fingerprint/PIN
   সেটআপ থাকলে অন করতে পারবেন। এই app lock ফাইলের password-এর বিকল্প নয়।

আগের debug APK-তে নতুনটি ইনস্টল না হলে uninstall করার আগে যাচাই করা encrypted backup নিন।
Uninstall করলে Private Vault ও Trash মুছে যায়।

## কম্পিউটার থেকে build

1. https://developer.android.com/studio থেকে Android Studio ইনস্টল করুন।
2. https://github.com/xjitroy/TXT-PAD → Code → Download ZIP, unzip করে project folder খুলুন।
3. JDK 17, Android SDK 36 ও Gradle Sync সম্পূর্ণ করুন। প্রথমবার dependency নামাতে internet লাগবে।
4. Build → Generate App Bundles or APKs → Generate APKs নির্বাচন করুন।
5. APK পাবেন app/build/outputs/apk/debug/app-debug.apk-এ। নিজের ফোনে নিয়ে ইনস্টল করুন।

Linux/macOS Terminal: ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
Windows-এ gradlew.bat বা Android Studio-র Gradle panel ব্যবহার করুন।

## শুধু ফোন থেকে GitHub Actions build

রিপোজিটরি আগেই আছে—নতুন repo লাগবে না। নিজের GitHub account থেকে
https://github.com/xjitroy/TXT-PAD/actions → TXT checks → Run workflow চাপুন।
সব check সবুজ হলে unsigned candidate তৈরি হবে। Signed release তৈরির নিয়ম docs/RELEASING.md-এ আছে। ব্যর্থ হলে failed step-এর log
ও TXT-check-reports artifact দেখুন।

## আসল ফাইল রাখার আগে

নিজের ফোনে বাংলা/ইংরেজি লিখে Auto-save, keyboard, Undo / Redo ও Save As পরীক্ষা করুন।
Lock দিয়ে সেভ করে বন্ধ করুন; সঠিক password দিয়ে খুলুন। Private Vault-এ move করুন।
Backup export করে Keep Both দিয়ে restore যাচাই করুন। Backup ফোনের বাইরে নিরাপদ স্থানে রাখুন।

অ্যাপের নিজস্ব INTERNET / ACCESS_NETWORK_STATE permission নেই। Build-এ internet লাগে,
ইনস্টল করা অ্যাপের কাজ স্থানীয়ভাবে হয়। System file picker-এ cloud provider বাছলে সেই provider-এর
নিজস্ব আচরণ প্রযোজ্য—সম্পূর্ণ স্থানীয় কাজের জন্য local folder বাছুন।

## Play Store ও পরের update

Release-এর TXT.aab Play Console-এ জমা দিন। TXT-store-assets.zip-এ logo, feature graphic,
screenshots ও listing text আছে। Privacy policy ও Console-এর owner-only কাজ docs/PLAY_STORE.md-এ দেখুন।
আপনার real contact/account তথ্য ও Play review GitHub build দিয়ে পূরণ হয় না।

পরের agent/AI-কে AGENTS.md, docs/HANDOFF.md ও docs/RELEASING.md পড়তে বলুন। একই signing key
ব্যবহার করতে হবে। Private signing kit বা password কোনো public repo/release/issue-তে দেবেন না।
GitHub Actions secrets না থাকলেও local signing + public artifact staging workflow ব্যবহার করা যায়।
