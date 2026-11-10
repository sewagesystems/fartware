# fartware

Client-side utility mod base for Fabric (Minecraft 1.21.1) with a click GUI, modules, hotkeys and chat commands.

## Usage

- **Right Shift** opens the click GUI (rebindable like any module).
  - Left click a module: toggle. Right click: expand its settings. Middle click (or the *Bind* row): bind a key (Esc/Backspace/Delete clears it).
  - Drag panel headers to move them, right click a header to collapse, scroll to move all panels.
- **Hotkeys** toggle modules while in-game with no screen open.
- **Commands** are typed in chat with the `.` prefix and never reach the server:

| Command | Description |
| --- | --- |
| `.help [command]` | List commands |
| `.toggle <module>` (`.t`) | Toggle a module |
| `.modules` | List modules |
| `.bind <module> <key>` | Bind a module (`r`, `f6`, `rshift`, `none`, ...) |
| `.unbind <module>` | Remove a bind |
| `.binds` | List binds |
| `.set <module> [setting] [value]` | Show / change settings |
| `.config <save\|load>` | Save / reload `config/fartware.json` |
| `.panic` | Disable every module |

Module state, binds and settings are saved to `config/fartware.json`.

## Fonts

`default.ttf` (Noto Sans, OFL) ships inside the jar and is copied to `<game dir>/fartware/fonts/` on first launch; an existing file is never overwritten, so you can replace it with your own. Drop any `.ttf` / `.otf` into that folder to use it.

- Click GUI: ClickGUI module → **Font** row (left click = next, right click = previous).
- Commands: `.font list`, `.font folder`, `.font set <name>`, `.font size <6-32>`, `.font vanilla`, `.font reset`.

## Adding things

- **Module**: extend `fartware.client.module.Module` (override `onTick`, `onRender2D`, `onEnable`, `onDisable`, add settings with `add(new BooleanSetting(...))`) and register it in `ModuleManager`. It shows up in the GUI, commands and config automatically.
- **Command**: extend `fartware.client.command.Command` and register it in `CommandManager`.
- The command prefix is `CommandManager.PREFIX`.

## Building

`./gradlew build` — the jar ends up in `build/libs`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
