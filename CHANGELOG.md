# CK_PB1 Changelog

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
