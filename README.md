# CK_PB1

**CK_PB1** یک کلاینت اختصاصی Minecraft Java (Fabric / Minecraft 1.20.1) است که شامل
Launcher حرفه‌ای، سیستم Module کامل، HUD قابل شخصی‌سازی، سیستم Update مبتنی بر
GitHub Releases و Installer ویندوز (`CK_PB1_Setup.exe`) می‌شود.

> **CK_PB1** نام کل کلاینت است؛ هیچ Module یا قابلیتی با این نام وجود ندارد
> (برای مثال «Kill Farm» فقط نام یک ماژول است).

---

## ⚠️ شرایط استفاده (مهم)

* قابلیت‌های **PvP** و **Automation** این کلاینت **فقط برای Singleplayer و سرورهای
  خصوصی/تستی** پیاده‌سازی شده‌اند که استفاده از چنین قابلیت‌هایی در آن‌ها مجاز است.
* استفاده در سرورهای عمومی ممکن است قوانین آن سرورها را نقض کند؛ مسئولیت استفاده
  با کاربر است.
* CK_PB1 وابسته به Mojang/Microsoft **نیست**. هیچ فایل Minecraft بازتوزیع نمی‌شود؛
  Launcher همه‌چیز را در زمان اجرا از سرورهای رسمی Mojang و Fabric دانلود می‌کند.
* Minecraft باید به‌صورت قانونی در دسترس شما باشد.

---

## ساختار Repository

```
CK_PB1
├── source      # کتابخانه مشترک CK_PB1 (نسخه‌ها، JSON، GitHub API)
├── launcher    # CK_PB1 Launcher (Java/Swing)
├── client      # کلاینت Fabric (هسته، Mixinها، HUD، GUI)
├── modules     # پیاده‌سازی ماژول‌ها (Combat, Movement, BedWars, ...)
├── installer   # NSIS Installer + آیکون + اسکریپت Build
├── configs     # پروفایل‌ها و کانفیگ‌های پیش‌فرض
├── releases    # یادداشت‌های انتشار هر نسخه
├── .github     # CI: Build کامل + انتشار خودکار Release
├── README.md
└── CHANGELOG.md
```

## دریافت و نصب (ویندوز)

1. از صفحه **Releases** این مخزن، آخرین نسخه را بگیرید:
   `CK_PB1-v1.0.0-Setup.exe` (همان `CK_PB1_Setup.exe`)
2. Installer را اجرا کنید؛ شامل:
   * انتخاب مسیر نصب
   * **Java 17 Runtime داخلی** (بدون نیاز به Java سیستمی) — یا در صورت وجود، Java سیستم 17+
   * میانبر Desktop و Start Menu
   * Uninstaller کامل (داده‌های کاربر حفظ می‌شود)
   * Upgrade نسخه‌های قبلی (نصب روی نسخه موجود)
3. **CK_PB1** را از میانبر اجرا کنید و **Launch** بزنید.

Launcher در اولین اجرا Minecraft 1.20.1 (فایل‌های رسمی Mojang)، Fabric Loader و
کلاینت CK_PB1 را دانلود و نصب می‌کند؛ دفعات بعدی به‌صورت آفلاین/کش‌شده اجرا می‌شود.

## CK_PB1 Launcher

| قابلیت | توضیح |
| --- | --- |
| انتخاب نسخه Minecraft | از Manifest رسمی Mojang (نسخه پشتیبانی‌شده: **1.20.1**) |
| انتخاب Java | جستجوی خوددی JVMها (17+) + Java Runtime همراه نصب‌شده |
| تنظیم RAM | اسلایدر ۱ تا ۱۶GB (با احترام به حافظه سیستم) |
| انتخاب Game Directory | پوشه دلخواه + پیش‌فرض `.minecraft` |
| Launch | نصب خودکار + اجرای بازی + کنسول زنده |
| Profiles | چند پروفایل مستقل (نسخه/Java/RAM/پوشه/نام کاربری) |
| Changelog | تاریخچه نسخه‌ها از GitHub Releases |
| Update Checker | مقایسه نسخه نصب‌شده با آخرین Release + بنر «آپدیت جدید در دسترس است» + دکمه دانلود و نصب |
| Download Manager | دانلود موازی با پیشرفت/سرعت/توقف/ادامه/Cancel |
| نمایش نسخه‌ها | نسخه فعلی و آخرین نسخه در هدر |

## کلاینت درون بازی

* **Click GUI** — کلید `RShift` (کشیدن پنل‌ها، جستجو، تنظیمات هر ماژول، کپچر Keybind با Middle-click)
* **HUD Editor** — از GUI یا دستور `.ck hud` (جابه‌جایی همه المان‌ها + Snap)
* **دستورات** — `.ck help` (alias `.ckpb1`): `gui`, `hud`, `toggle`, `bind`, `profile`, `tp`, `targets`, `beds`, `info`
* **پروفایل‌ها** — ذخیره/بارگذاری کل تنظیمات ماژول‌ها در `config/ckpb1/profiles/`

### دسته‌ها و ماژول‌ها

| دسته | ماژول‌ها |
| --- | --- |
| **Combat** | Combat Assistant, Target Selector, **Kill Farm**, CPS Counter, Reach Display, Hit Information, Combo Information, Projectile Information, Auto Fireball Defense |
| **Movement** | Movement Assistant, Fly |
| **Player** (تست) | Infinite Health, No Fall Damage, Fire Resistance, Teleport |
| **Render** | Crosshair, Fullbright |
| **World** | Resource Assistant |
| **Utility** | Auto Clicker, Auto Tool |
| **BedWars** | Bed Destroyer V1, **Bed Destroyer V2**, Bridge Assistant |
| **Automation** | BedWars Automation |
| **HUD** | Watermark, FPS, Ping, CPS, Coordinates, Keystrokes, Armor Status, Potion Status, Target HUD, Session Info, BedWars Status |

### Kill Farm (توضیح کامل)

فقط برای Singleplayer/سرور تستی: **Target List** چند بازیکن (از GUI یا `.ck targets add name`)،
اولویت (ترتیب لیست / نزدیک‌ترین / کم‌جان)، **Target Switching** با تأخیر قابل تنظیم،
**Target Tracking** (چرخش به سمت هدف)، حالت Single/Switch، توقف خودکار وقتی هیچ هدفی
در دسترس نباشد، Keybind (پیش‌فرض `K`) و **HUD Target** اختصاصی.

### Bed Destroyer V1 / V2

* **V1**: تشخیص تخت و تخت دشمن، تحلیل Defense (obsidian/end stone/...)، مسیریابی A*،
  نمایش مسیر، شکستن تخت، وضعیت زنده روی HUD.
* **V2**: اسکن همه تخت‌ها + لیست قابل انتخاب (`.ck beds`)، اولویت (Distance/Defense/Manual)،
  **Target Lock**، مسیریابی، و برای محیط تست: **Teleport** و **Instant Destroy** (فقط Singleplayer).

### Bridge Assistant

انواع Bridge (God Bridge / Breezily / Ninja)، سرعت قابل تنظیم، **Edge Detection**،
توقف اضطراری (Keybind)، **Path Preview** سه‌بعدی، شمارش بلوک باقی‌مانده.

### BedWars Automation (تست)

ماشین حالت قابل تنظیم: `Collect → Buy → Bridge → Attack → Destroy` — هر مرحله
قابل فعال/غیرفعال‌سازی و پیکربندی است؛ وضعیت Match روی HUD نمایش داده می‌شود.

### Test God Mode (فقط Singleplayer/تست)

هر قابلیت ماژول جداگانه دارد: **Infinite Health**، **No Fall Damage**،
**Fire Resistance**، **Fly**، **Teleport**.

## Performance

* بدون وابستگی به Fabric API (فقط Fabric Loader) → مصرف RAM/CPU کمینه
* اسکن بلوک‌ها و مسیریابی با Budget زمانی (بدون افت FPS)
* HUD و رندر سه‌بعدی با overlay سبک (خطوط DEBUG_LINES)
* Fullbright بدون سمبل‌های اضافه، Crosshair سبک، بدون رندرهای سنگین

## Build از سورس

```bash
./gradlew :client:build :launcher:jar     # خروجی: client/build/libs، launcher/build/libs
bash installer/build.sh                   # نیاز به JDK 17 + NSIS → dist/CK_PB1_Setup.exe
java -jar launcher/build/libs/CK_PB1-Launcher-1.0.0.jar --selftest   # تست سرویس‌ها
```

CI (GitHub Actions) روی هر Push کل پروژه را Build و تست می‌کند و روی Push تگ `v*`
نسخه Release را با فایل‌های `CK_PB1-v*-Setup.exe`، `CHANGELOG.md` و `README.md` منتشر می‌کند.

## مجوز

کد این پروژه تحت مجوز **MIT** منتشر شده (فایل `LICENSE`). Minecraft علامت تجاری
Mojang Synergies AB است و این پروژه هیچ نسبتی با آن ندارد.

---

*CK_PB1 v1.0.0 — Minecraft 1.20.1 (Fabric)*
