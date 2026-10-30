# TXT: APK তৈরির সহজ নিয়ম

GitHub repo বাধ্যতামূলক নয়। APK এখনও build করা হয়নি।

## কম্পিউটার থাকলে — সবচেয়ে সহজ

1. https://developer.android.com/studio থেকে Android Studio ইনস্টল করুন। Setup Wizard-এ Android SDK ইনস্টল হতে দিন।
2. TXT-project.zip আনজিপ করে Android Studio → Open → TextPad folder খুলুন।
3. প্রথমবার ইন্টারনেট চালু রাখুন। Gradle Sync ও SDK 35 ইনস্টল শেষ হতে দিন। SDK license চাইলে গ্রহণ করুন।
4. Build → Generate App Bundles or APKs → Generate APKs নির্বাচন করুন। Android Studio সংস্করণে মেনুর নাম সামান্য আলাদা হতে পারে।
5. APK পাবেন app/build/outputs/apk/debug/app-debug.apk-এ। USB বা স্থানীয় ফাইল ট্রান্সফারে ফোনে নিন।
6. ফোনে APK খুলে ইনস্টল করুন। এই debug APK পরীক্ষার জন্য; Play Store release-এর জন্য নিজের signing key লাগবে।

বিকল্প Terminal command: ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
Windows-এ Android Studio-র Gradle panel থেকে একই task চালাতে পারেন; POSIX bootstrap script Windows CMD-র জন্য নয়।

## শুধু ফোন থাকলে — GitHub Actions

1. GitHub account-এ একটি private repository তৈরি করুন। ZIP-এর ভিতরের TextPad folder-এর contents repo root-এ দিন: app/, settings.gradle.kts, build.gradle.kts ইত্যাদি। ZIP ফাইল একা upload করলে build হবে না।
2. .github/workflows/android.yml ফাইলটিও একই path-এ রাখুন। ফোনে hidden folder upload অসুবিধা হলে GitHub → Add file → Create new file দিয়ে ওই পূর্ণ path লিখে project-এর workflow-এর content paste করুন।
3. Actions → Build TXT APK → Run workflow চাপুন।
4. সব check সবুজ হলে completed run-এর Artifacts → TXT-debug-APK ডাউনলোড করুন। Artifact ZIP খুলে APK ইনস্টল করুন।
5. Build লাল হলে TXT-check-reports ও failed step-এর log দেখুন। এখানে build পরীক্ষা করা হয়নি, তাই সবুজ build নিশ্চিত বলে ধরে নেবেন না।

## ইনস্টলের পর সংক্ষিপ্ত পরীক্ষা

Workspace folder বাছুন। New File → কয়েক লাইন লিখুন → Back → ফাইল আবার খুলুন। Lock দিয়ে password সেট করুন, ভুল password ব্যর্থ হয় এবং সঠিক password কাজ করে দেখুন। Private Vault-এ move করুন। Backup export করে Keep Both দিয়ে restore পরীক্ষা করুন। Settings থেকে fingerprint lock চালু/বন্ধ পরীক্ষা করুন। আসল মূল্যবান ফাইল রাখার আগে নিজের ফোনে এই পরীক্ষা করুন।

অ্যাপের নিজস্ব network permission নেই। প্রথম build-এ dependency নামাতে ইন্টারনেট লাগে। GitHub/Android Studio-র বাইরে অ্যাপ চালাতে ইন্টারনেট লাগে না।
