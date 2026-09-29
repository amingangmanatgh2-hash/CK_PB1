# CK_PB1 Configs

این پوشه کانفیگ‌های نمونه/پیش‌فرض CK_PB1 را نگه می‌دارد.

## ساختار کانفیگ داخل بازی

کلاینت در اولین اجرا این ساختار را در `<game dir>/config/ckpb1/` می‌سازد:

```
config/ckpb1/
├── active-profile.txt        # نام پروفایل فعال
├── hud-layout.json           # موقعیت المان‌های HUD
└── profiles/
    └── default.json          # وضعیت و تنظیمات همه ماژول‌ها
```

* هر پروفایل شامل: وضعیت روشن/خاموش، Keybind و همه‌ی تنظیمات هر ماژول است.
* مدیریت پروفایل‌ها از داخل بازی: `.ck profile list|new|load|delete <name>`
* Layout با هر جابه‌جایی در HUD Editor ذخیره می‌شود.

## default/ckpb1-default-profile.json

نمونه‌ی پروفایل پیش‌فرض (همان چیزی که کلاینت در اولین اجرا می‌سازد).
می‌توانید آن را در `config/ckpb1/profiles/` کپی و از `.ck profile load` بارگذاری کنید.
