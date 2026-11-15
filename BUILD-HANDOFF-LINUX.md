# Сборка ttcuz на Linux

В корне каталога `MargyT/` находятся исходники версии 0.6.8.13 Google Icons и обычный TikTok APK 47.2.41. Персональные ключи подписи находятся в `private-keys/` внутри переносного архива.

## Требования

- Python 3
- JDK 17 или новее
- Интернет при первой сборке: сборщик загрузит инструменты Android API 35 в `tools/`

## Защищённая сборка

Из корня `MargyT/` выполните:

```sh
export TTCUZ_INTEGRITY_KEY="$PWD/private-keys/integrity.p12"
export TTCUZ_INTEGRITY_PASSWORD="$(cat private-keys/integrity-password.txt)"
python3 -m ttcuz apks/tiktok-47-2-41.apk --keystore private-keys/stable-debug.jks
```

Keystore использует alias `androiddebugkey`; пароль keystore и ключа — стандартный `android`. Готовый APK появится в `build/ttcuz.apk`.

Архив включает снимок файлов без Git-истории и закрытые ключи подписи с паролем целостности. Храните его приватно; пароль не вставляйте в чат и не загружайте архив в публичное хранилище.