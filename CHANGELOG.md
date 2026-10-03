# CK_PB1 Changelog

## v1.1.0 — 2026-10-03

آپدیت بزرگ «قابلیت‌ها یک لول بالاتر» — ۱۲ ماژول جدید + ارتقای Fly و Bridge و منو.

### جدید در v1.1.0
* **Legit Aura** — کیل‌اورای لگیت سبک 1.8.9: روتیشن نرم با Jitter، CPS گاوسی، FOV، Wall Check،
  دروازه‌ی سوئینگ فقط وقتی کراس‌هیر روی هدف است، تأخیر سوئیچ هدف و Micro-Pause انسانی
* **Scaffold** — گذاشتن خودکار بلوک زیر پا هنگام دویدن (Normal/Tower)، Auto Slot و Keep Rotation
* **God Bridge (0 CPS)** — حالت جدید Bridge Assistant: بریج خودکار با سرعت کامل بدون حتی یک کلیک
* **Fly سه‌حالته** — Creative (کلاسیک)، Jetpack (سرعتی نرم بدون دست‌زدن به Abilities)،
  Glide (سرنشین نرم) + Anti-Kick برای چک‌های Floating ونیلا
* **ESP** — هایلایت Players / Mobs / Items / Chests با رنگ‌های جدا (اسکن تخت‌ها کش می‌شود)
* **Tracers** — خط از چشم تا بازیکنان/ماب‌ها
* **Trajectories** — پیش‌بینی مسیر پرتابه (Bow, Snowball, Egg, Ender Pearl, Potion, XP Bottle) + نشانگر فرود
* **Chat Translator** — ترجمه خودکار پیام‌های چت با گوگل (fa/en/ar/tr/fr/de/ru/es)
* **Anti AFK** — حرکات کوچک تصادفی (Rotate/Sneak/Mixed) برای نرفتن AFK
* **Auto Respawn** — ریسپان خودکار بعد از مرگ با تأخیر قابل‌تنظیم
* **No Slow** — حذف کندی حرکت هنگام خوردن/استفاده آیتم (mixin)
* **Fast Place** — حذف تأخیر ۴ تیکی ونیلا بین قرار دادن بلوک‌ها
* **Spider** — بالا رفتن از دیوارها مثل عنکبوت
* **Auto Sprint** — اسپرینت خودکار هنگام حرکت به جلو
* **Module List HUD** — لیست Array جدید با گرادیان رنگی
* **منوی جدید** — گرادیان، نوار وضعیت پایین، رنگ اختصاصی هر دسته، شمارنده‌ی ماژول‌های فعال،
  تایپ = جستجوی فوری، کپچر کی‌بیند با کادر مخصوص

### رفع اشکال
* رفع ۴۹ خطای کامپایل نسخه قبل (ColorHelper, RaycastContext, EntityHitResult, ...)
* رفع باگ Parse کردن Manifest ماینکرفت (کلید versions)
* رفع NSIS روی لینوکس (مسیرهای OutFile)


## v1.0.0 — 2026-09-29

اولین نسخه رسمی CK_PB1.

### کلاینت (Minecraft 1.20.1 / Fabric)
* سیستم Module کامل: Enable/Disable، Keybind، Settings، توضیحات، ذخیره تنظیمات، پروفایل‌ها
* Click GUI (RShift) با پنل‌های قابل جابه‌جایی، جستجو، تنظیمات زنده و کپچر Keybind
* HUD Editor با کشیدن/اسنپ المان‌ها + ذخیره Layout
* ۱۱ المان HUD: Watermark, FPS, Ping, CPS, Coordinates, Keystrokes, Armor Status, Potion Status, Target HUD, Session Info, BedWars Status
* Combat: Combat Assistant, Target Selector, Kill Farm (Target List/Priority/Switching/Tracking/توقف خودکار/HUD Target), CPS Counter, Reach Display, Hit/Combo/Projectile Information, Auto Fireball Defense, Crosshair
* Movement: Movement Assistant, Fly
* Player (تست): Infinite Health, No Fall Damage, Fire Resistance, Teleport
* Render: Crosshair (استایل/رنگ/اندازه), Fullbright
* World: Resource Assistant (Iron/Gold/Diamond/Emerald + مسیریابی)
* BedWars: Bed Destroyer V1 و V2 (لیست تخت، اولویت، Target Lock، مسیریابی، Teleport/Instant Destroy تستی)، Bridge Assistant (God Bridge/Breezily/Ninja)
* Automation: BedWars Automation (ماشین حالت Collect→Buy→Bridge→Attack→Destroy)
* Utility: Auto Clicker, Auto Tool
* دستورات `.ck` / `.ckpb1`

### Launcher
* انتخاب نسخه Minecraft (از Manifest رسمی)، انتخاب Java (17+)، تنظیم RAM، Game Directory
* نصب خودکار Minecraft + Fabric Loader + کلاینت CK_PB1 (دانلود از سرورهای رسمی)
* Profiles، Changelog (از GitHub Releases)، Update Checker با بنر «آپدیت جدید در دسترس است»
* Download Manager موازی با Pause/Resume/Cancel
* کنسول بازی زنده، UI فارسی (RTL)

### Installer
* `CK_PB1_Setup.exe` واقعی (NSIS): انتخاب مسیر، Desktop/Start Menu Shortcut،
  Java 17 Runtime داخلی، Uninstaller کامل، Upgrade نسخه قبلی، بررسی Dependencies

### زیرساخت
* CI کامل (Build + Self-test + انتشار خودکار Release روی تگ)

---

## Roadmap (نسخه‌های بعدی)

* **v1.1.0**: بهبود مسیریابی (diagonal + parkour)، ماژول‌های اضافی Render، بکاپ/restore کانفیگ از GUI
* **v1.2.0**: پشتیبانی از چند نسخه Minecraft همزمان، مدیریت چند اکانت، فِرِیم‌ور ضد‌افت FPS پیشرفته‌تر
