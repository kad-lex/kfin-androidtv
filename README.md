<p align="center">
<img alt="KFIN" src="docs/kfin_logo.png" width="360"/>
</p>
<h3 align="center">Форк <a href="https://github.com/jellyfin/jellyfin-androidtv">Jellyfin Android TV</a> с переработанным интерфейсом</h3>

---

KFIN — это Jellyfin для Android TV с полностью переосмысленным внешним видом: единый тёмный стиль без лишних подложек, акцентные цвета, взятые из постеров, полупрозрачный плеер и внимание к плавности работы на слабом железе.

Пакет приложения — `org.kfin.androidtv`: устанавливается отдельно и не конфликтует с оригинальным Jellyfin.

## Скриншоты

<!-- СКРИН 1: главный экран (карусель + ряды) -->
![Главный экран](docs/screenshots/home.png)

<!-- СКРИН 2: библиотека (сетка постеров, статус-бар с капсулами) -->
![Библиотека](docs/screenshots/library.png)

<!-- СКРИН 3: страница фильма/сериала (описание, кнопки, плашки) -->
![Страница контента](docs/screenshots/details.png)

<!-- СКРИН 4: сезоны/серии плашками с постерами -->
![Сезоны и серии](docs/screenshots/seasons.png)

<!-- СКРИН 5: страница актёра -->
![Страница актёра](docs/screenshots/person.png)

<!-- СКРИН 6: плеер с панелью управления -->
![Плеер](docs/screenshots/player.png)

<!-- СКРИН 7: меню плеера (качество/дорожки/субтитры) -->
![Меню плеера](docs/screenshots/player_menu.png)

## Ключевые особенности

### Интерфейс

- **Карусель на главном экране** — полноширокая лента рекомендуемого контента с кинематографичным бэкдропом: логотип тайтла, кнопка «Смотреть» и точечный индикатор страниц; под ней сразу ряд «Продолжить просмотр».
- **Статус-бар без подложки** — капсулы библиотек, поиск, настройки, часы и пользователь прямо на фоне; в покое чёрные, при фокусе — статичное белое выделение с тёмным содержимым.
- **Карточки** — белая обводка в покое, акцентный цвет постера при фокусе, кружок воспроизведения по центру; бейджи количества серий и индикации просмотра с 90% непрозрачности.
- **Страница контента** — серии и сезоны плашками: название слева, описание в две строки с многоточием (плашка не растягивается), портретный постер со скруглением справа.
- **Страница актёра** — фото фиксированного размера со скруглением, имя справа от фото; сетка актёров 6 в ряд.
- **Единая геометрия** — заголовок, описание, кнопки, списки и сетки выровнены по общей границе 28dp на всех страницах.
- **Меню в едином стиле** — чёрные с мягким контуром: настройки, сортировка, контекстное меню (удержание OK), выбор внешнего плеера; без затемнения фона.

### Плеер

- Чёрная панель у нижнего края экрана, все элементы с **60% непрозрачности** (логотип — без прозрачности).
- Верхний ряд: перемотка, воспроизведение, время; нижний ряд: дорожки, субтитры, качество, скорость, масштаб — кнопки-капсулы с инверсией иконки при фокусе.
- Название и время над прогресс-баром; меню качества/дорожек/субтитров открываются над панелью фиксированными ячейками.
- Встроенный движок — **ExoPlayer (Media3)**; внешние плееры (VLC, MX Player, mpv, Vimu) поддерживаются через отдельную кнопку.

### Производительность

- Не более 2 параллельных декодов изображений (Coil) — скролл сеток без пропусков кадров на слабых ТВ-боксах.
- Memory cache зафиксирован на 25% хипа (без автоурезания на low-RAM устройствах).
- Серверное уменьшение картинок: бэкдропы до 1920px, превью до 1280px, логотипы до 640px — без 4K-оригиналов.
- Блюр фона заменён акцентной заливкой, у карточек нет теней и зума — только анимация обводки.
- Релизные сборки: R8 + сжатие ресурсов (~30 МБ APK).

### Платформа

- minSdk 23 (Android 6+), целевая платформа — Android 11 и новее, приоритет — ТВ-боксы.
- Совместимость с сервером Jellyfin 10.9+.

## Установка

Скачайте APK из раздела [Releases](https://github.com/kad-lex/kfin-androidtv/releases) и установите на устройство. Для обновлений используется тот же пакет — новые версии ставятся поверх.

## Patch notes

### v0.2.0-rc.2 (pre-release) — первый публичный пререлиз

**Интерфейс**
- Статус-бар: капсулы библиотек, часы и пользователь без подложки, статичное белое выделение.
- Карточки: обводка с акцентом постера при фокусе, кружок воспроизведения, бейджи 90% непрозрачности.
- Страница контента: список серий/сезонов плашками с портретными постерами, описание с многоточием.
- Страница актёра: фиксированное фото со скруглением, имя справа от фото.
- Сетка актёров 6 в ряд, выравнивание всего по общей границе 28dp.
- Плеер: чёрная панель у нижнего края, элементы с 60% непрозрачности (кроме логотипа), кнопки-капсулы, меню над панелью.
- Меню в едином стиле: чёрные с мягким контуром, без затемнения фона.

**Производительность**
- Coil: не более 2 параллельных декодов, memory cache 25% хипа.
- Серверное уменьшение картинок: бэкдропы 1920, превью 1280, логотипы 640.
- Плеер: стилизация кнопок только при смене состава (без обходов дерева на каждый кадр).

**Исправления**
- Имя актёра больше не обрезается за фото; у всех актёров одинаковый размер фото.
- Постеры в плашках сезонов получили видимое скругление (портретный слот с заполнением).
- Навигация по сетке актёров: движение строго по строкам/столбцам, без диагоналей.

### v0.2.0-rc.3 (pre-release)

- Исправлен краш плеера при переходах (`Fragment ... has already been destroyed`).
- «Проверить обновления» в Настройки → О приложении (открывает страницу релизов на GitHub).
- README со скриншотами и описанием.

## Сборка из исходников

```bash
# debug
./gradlew :app:assembleDebug -Pjellyfin.version=0.2.0

# release (нужен keystore, см. signingConfigs в app/build.gradle.kts)
./gradlew :app:assembleRelease -Pjellyfin.version=0.2.0
```

Требования: JDK 21, Android SDK.

## Благодарности и лицензия

KFIN основан на [Jellyfin Android TV](https://github.com/jellyfin/jellyfin-androidtv) и наследует его лицензию [GPL 2.0](LICENSE). Вся команда Jellyfin — авторам апстрима.

---

<h3 align="center">English</h3>

KFIN is Jellyfin for Android TV with a completely reimagined look: a unified dark style without unnecessary backdrops, accent colors taken from posters, a translucent player, and a focus on smooth performance on low-end hardware.

The application package is `org.kfin.androidtv`: it installs separately and does not conflict with the original Jellyfin app.

## Screenshots

![Home screen](docs/screenshots/home.png)
![Library](docs/screenshots/library.png)
![Details page](docs/screenshots/details.png)
![Seasons and episodes](docs/screenshots/seasons.png)
![Person page](docs/screenshots/person.png)
![Player](docs/screenshots/player.png)
![Player menu](docs/screenshots/player_menu.png)

## Key features

### Interface

- **Home screen carousel** — a full-width strip of featured content with a cinematic backdrop: title logo, a "Watch" button, and a dotted page indicator; the "Continue Watching" row sits right below it.
- **Backdrop-free status bar** — library capsules, search, settings, clock and user straight on the background; black at rest, static white highlight with dark content on focus.
- **Cards** — white outline at rest, poster accent color on focus, centered play bubble; episode-count and watched badges at 90% opacity.
- **Details page** — episodes and seasons as pills: title on the left, two-line description with ellipsis (the pill never stretches), rounded portrait poster on the right.
- **Person page** — fixed-size rounded photo with the name at its right edge; a 6-per-row cast grid.
- **Unified geometry** — title, description, buttons, lists and grids all share the same 28dp edge on every page.
- **Consistent menus** — black with a soft outline: settings, sorting, context menu (long-press OK), external player picker; no background dimming.

### Player

- Black panel at the bottom edge of the screen, all elements at **60% opacity** (the logo stays opaque).
- Top row: rewind, play/pause, fast-forward, time; bottom row: audio tracks, subtitles, quality, speed, zoom — capsule buttons with icon inversion on focus.
- Title and time above the progress bar; quality/track/subtitle menus open above the panel with fixed-size cells.
- Built-in engine — **ExoPlayer (Media3)**; external players (VLC, MX Player, mpv, Vimu) are supported via a dedicated button.

### Performance

- At most 2 parallel image decodes (Coil) — grid scrolling without dropped frames on weak TV boxes.
- Memory cache pinned at 25% of the heap (no auto-shrinking on low-RAM devices).
- Server-side image scaling: backdrops down to 1920px, thumbnails to 1280px, logos to 640px — no 4K originals.
- Background blur replaced with an accent fill; no card shadows or zoom — border animation only.
- Release builds: R8 + resource shrinking (~30 MB APK).

### Platform

- minSdk 23 (Android 6+), target platform — Android 11 and newer, with a focus on TV boxes.
- Compatible with Jellyfin server 10.9+.

## Installation

Download the APK from the [Releases](https://github.com/kad-lex/kfin-androidtv/releases) section and install it on your device. Updates install on top using the same package.

## Patch notes

### v0.2.0-rc.3 (pre-release)

- Fixed a player crash on transitions (`Fragment ... has already been destroyed`).
- "Check for updates" in Settings → About (opens the GitHub releases page).
- README with screenshots and description.

### v0.2.0-rc.2 (pre-release) — first public pre-release

**Interface**
- Status bar: library capsules, clock and user without backdrops, static white highlight.
- Cards: poster accent outline on focus, play bubble, 90%-opacity badges.
- Details page: episodes/seasons list as pills with portrait posters, ellipsis description.
- Person page: fixed-size rounded photo, name at the photo's right edge.
- 6-per-row cast grid, everything aligned to the common 28dp edge.
- Player: black panel at the bottom edge, 60% opacity elements (except the logo), capsule buttons, menus above the panel.
- Consistent menus: black with a soft outline, no background dimming.

**Performance**
- Coil: max 2 parallel decodes, memory cache at 25% heap.
- Server-side image scaling: backdrops 1920, thumbnails 1280, logos 640.
- Player: buttons restyled only when the layout structure changes (no per-frame tree walks).

**Fixes**
- Actor names no longer clipped behind the photo; all actors get the same photo size.
- Season pill posters got visible rounding (portrait slot with fill).
- Cast grid navigation: strictly row/column movement, no diagonals.

## Building from source

```bash
# debug
./gradlew :app:assembleDebug -Pjellyfin.version=0.2.0

# release (a keystore is required, see signingConfigs in app/build.gradle.kts)
./gradlew :app:assembleRelease -Pjellyfin.version=0.2.0
```

Requirements: JDK 21, Android SDK.

## Credits and license

KFIN is based on [Jellyfin Android TV](https://github.com/jellyfin/jellyfin-androidtv) and inherits its [GPL 2.0](LICENSE) license. All credit for the upstream goes to the Jellyfin team.
