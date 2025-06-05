# Item Tooltip Enhancer

Item Tooltip Enhancer is a Forge mod for Minecraft 1.20.1 that expands the information shown when hovering over items. It adds features inspired by popular RPG servers such as customizable rarity colors, formatted statistics, and configurable tooltips. The mod was originally created for the Flaze SMP server but is designed to be generic and configurable for any Forge server.

## Features

- **Custom item rarities** – Items are assigned rarities (Common, Uncommon, Rare, Epic, Legendary, Mythic, Special, Admin) that control the color of the name and rarity line in the tooltip.
- **Configurable tooltips** – Per-item JSON files allow you to change an item's display name, category, rarity, tooltip lines, and more.
- **Stat formatting** – Optional Hypixel-style stat formatting with a configurable list of attribute names.
- **Automatic clearlag** – Built‑in clearlag system to periodically remove ground items. Players can choose how they are notified (chat, action bar, or none).
- **Server commands** – Commands for editing item data, dumping item info, scheduling clearlag, reloading configs, and more. See `/clearlag help` and `/flazesmpitems` in game.

## Building

Java 17 is required. Once installed, run:

```bash
./gradlew build
```

The built jar will be located under `build/libs/`.

## Contributing

Contributions are welcome! If you find a bug or have an idea for a feature, feel free to open an issue or submit a pull request. When developing locally, you may need to run:

```bash
./gradlew IntelliJRuns
```

After importing the Gradle project in IntelliJ or Eclipse, refresh the Gradle tasks to ensure the run configurations are generated. Example item configuration files can be found in the `config/itemtooltipenhancer` directory once the mod runs for the first time.

When submitting code please follow existing formatting and include useful log messages where appropriate. PRs that add new configuration options or commands are especially appreciated.

## License

This project is released under the MIT License. See [LICENSE](LICENSE) for details.
