# GameBoost

একটি Android অ্যাপ যা গেম খেলার সময় কাজে লাগে এমন কিছু বাস্তবসম্মত ইউটিলিটি দেখায়।

## যা আছে (বাস্তবে কাজ করে)

| ফিচার | কীভাবে কাজ করে |
|---|---|
| **FPS Overlay** | স্ক্রিনের উপর একটি ছোট floating counter, `Choreographer` দিয়ে প্রতি সেকেন্ডে ফ্রেম গোনে। এটা সিস্টেম-ওয়াইড ফ্রেম রেট দেখায় (root ছাড়া কোনো অ্যাপ অন্য অ্যাপের ভেতরের GL/Vulkan render FPS সরাসরি পড়তে পারে না — এটা সব non-root FPS overlay অ্যাপের সীমাবদ্ধতা)। |
| **CPU Usage** | `/proc/stat` থেকে পড়া হয়। কিছু OEM (Samsung/MIUI-এর কিছু ভার্সন) এটা ব্লক করে রাখে; ব্লক থাকলে "N/A" দেখাবে, ভুয়া সংখ্যা দেখানো হয় না। |
| **Battery %** | `BatteryManager` API, সব ফোনেই কাজ করে। |
| **Free RAM** | `ActivityManager.MemoryInfo`, সিস্টেম-ওয়াইড ফ্রি RAM (root লাগে না)। |
| **Gaming Mode** | Android-এর Do Not Disturb (Interruption Filter) চালু/বন্ধ করে — নোটিফিকেশন সাইলেন্ট করে দেয়। |
| **Volume Control** | Media ভলিউম স্লাইডার। |

## যা ইচ্ছা করেই রাখা হয়নি (কেন)

- **GPU Temperature** — non-root Android-এ পড়ার কোনো পাবলিক API নেই।
- **RAM Cleanup / "Boost"** — Android 5+ থেকে কোনো সাধারণ অ্যাপ অন্য অ্যাপকে kill করতে পারে না (`killBackgroundProcesses` শুধু নিজের cached process বন্ধ করে, বাস্তবে কোনো পারফরম্যান্স লাভ হয় না)। মার্কেটের বেশিরভাগ "cleaner" অ্যাপ placebo — এখানে ভুয়া ফিচার রাখা হয়নি।
- **FPS Boost / CPU Overclock** — root ছাড়া CPU/GPU governor বদলানো সম্ভব না।
- **Voice Changer** — এটা গেমিং পারফরম্যান্স ফিচার না, সম্পূর্ণ আলাদা অডিও প্রসেসিং মডিউল, এই প্রজেক্টের স্কোপে নেই।

চাইলে root-based ভার্সন বা voice changer আলাদা মডিউল হিসেবে পরে যোগ করা যাবে, কিন্তু root অ্যাক্সেস ছাড়া ফোনে কাজ করবে না এবং Play Store-এ পাবলিশ করা যাবে না।

## কীভাবে বিল্ড করবেন

### অপশন ১: GitHub Actions দিয়ে অটোমেটিক (রেকমেন্ডেড)
1. এই পুরো ফোল্ডারটা একটা নতুন GitHub রিপোজিটরিতে আপলোড/পুশ করুন।
2. রিপোর **Actions** ট্যাবে যান — `Build APK` workflow নিজে থেকেই রান হবে (main ব্রাঞ্চে পুশ করলে), অথবা "Run workflow" বাটনে ক্লিক করে ম্যানুয়ালি চালান।
3. বিল্ড শেষ হলে, workflow run-এর নিচে **Artifacts** সেকশন থেকে `GameBoost-debug-apk` ডাউনলোড করুন — এটাই আপনার `.apk` ফাইল।

### অপশন ২: Android Studio দিয়ে নিজে বিল্ড
1. Android Studio (Hedgehog বা তার পরের ভার্সন) দিয়ে এই ফোল্ডার Open করুন।
2. প্রথমবার Open করলে Android Studio নিজে থেকে Gradle wrapper তৈরি করে নেবে (Sync হওয়ার সময়)।
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`।

## ইনস্টল করার পর প্রথমবার যা করতে হবে
অ্যাপ খোলার পর দুটো পারমিশন সিস্টেম সেটিংসে গিয়ে দিতে হবে (Android নিরাপত্তার কারণে জোর করে দেয় না):
1. **Display over other apps** — FPS overlay দেখানোর জন্য।
2. **Do Not Disturb access** — Gaming Mode-এর জন্য।

অ্যাপ নিজেই এই সেটিংস পেজে নিয়ে যাবে যখন আপনি প্রথমবার বাটনে চাপ দেবেন।

## সাপোর্টেড ভার্সন
Android 7.0 (API 24) থেকে সর্বশেষ Android পর্যন্ত সব ফোনে কাজ করার জন্য বানানো হয়েছে।
