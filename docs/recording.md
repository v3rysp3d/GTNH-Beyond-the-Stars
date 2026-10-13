# In-game video recorder (dev only)

A dev-only recorder that films the dev client from a shot script and writes an mp4. It is inert unless the client
JVM runs with `-Dbts.rec=true`: the client proxy registers it only then, and in normal play none of its classes load.
Code: `src/main/java/com/nateyburr/beyondthestars/rendertest/Rec*.java` and `FrameRecorder.java`. Example scripts:
`tools/recording/*.shots`.

## What it does

1. It locks the window to the video size (1920x1080 by default), hides the HUD, the hand and the debug screen, and
   turns off view bobbing and vsync.
2. From the main menu it creates a fresh creative superflat world (`run/client/saves/bts-rec-<time>`) on the
   integrated server, the same way the autoshots photo harness does. No server port is used.
3. It runs the script's setup lines, then the scenes in order.
4. While a recording command runs, every rendered frame is read with `glReadPixels` (after the world, the GUI and the
   recorder's own caption are drawn) and piped as raw RGB to an ffmpeg child process. Each scene gets its own
   ffmpeg process and segment file.
5. At the end the segments are joined with ffmpeg's concat demuxer (`-c copy`) into one mp4, and the client closes.

**Real speed.** Frames go out on a fixed wall-clock schedule (30 fps by default). If the game renders slower, the last
frame is repeated; if it renders faster, the extra frames are not sent. So one second of game time is one second of
video, apart from time-lapses. `rec.log` gives the render fps and the number of repeated frames for each scene.

**Cuts.** Only the recording commands put frames in the video: `card`, `hold`, `orbit`, `dolly`, `pan` and
`timelapse`. Everything else happens between recorded spans and is cut out. That includes `goto` (teleport, then wait
for the chunks to arrive and render), GUIs opening, builds and waits.

**The camera** is the player, flying. During `orbit`, `dolly` and `pan` the client places it on every rendered frame
(position and rotation, with nothing left to interpolate), so moves are smooth at any frame rate, not stepped per tick.
The mouse is released, so moving it does not turn the view.

## Running it

```
cd GTNH-Projects/addon-autoshots
JAVA_TOOL_OPTIONS="-Dbts.rec=true -Dbts.rec.script=tools/recording/smoke.shots -Dbts.rec.ffmpeg=C:/path/to/ffmpeg.exe" ./gradlew.bat runClient --no-daemon
```

This buildscript's `runClient` takes no JVM-argument property, but `JAVA_TOOL_OPTIONS` reaches the forked client (as for
the render harness). Do not run it while another gradle build is running in the same worktree. The client closes
itself when the script is done.

Output (default `run/recordings/`, git-ignored with the rest of `run/`):

- `run/recordings/<script>-<yyyyMMdd-HHmmss>.mp4`: the finished video.
- `run/recordings/<script>-<yyyyMMdd-HHmmss>/`: the segments (`NN_<scene>.mp4`, each with an ffmpeg `.log`), a copy
  of the script, `concat.txt`, and `rec.log` (every command, the camera moves and a per-scene summary: video seconds,
  frames rendered and repeated, render fps).

### Properties

| Property | Default | Meaning |
|---|---|---|
| `bts.rec` | off | The switch. Nothing loads without `true`. |
| `bts.rec.script` | (required) | The shot script. A relative path is looked up from the client's working directory (`run/client`) and from the worktree root. |
| `bts.rec.ffmpeg` | `ffmpeg` | Path to ffmpeg (7.x with libx264, and h264_nvenc if you want it). The binary is not in the repo. |
| `bts.rec.out` | `run/recordings` | Output folder. |
| `bts.rec.size` | `1920x1080` | Window and video size, e.g. `1280x720`. On the dev machine (RTX 4090) the game renders 500 to 800 fps at 1080p while recording, far above the 30 needed. |
| `bts.rec.fps` | `30` | Video frame rate. |
| `bts.rec.encoder` | `auto` | `nvenc` (h264_nvenc, on the GPU), `x264` (libx264 `-preset veryfast`), or `auto` (nvenc if a test encode works, else x264). |
| `bts.rec.crf` | `20` | Quality: x264 `-crf`, nvenc `-cq`. |
| `bts.rec.only` | all | Comma-separated scene-name prefixes; other scenes are skipped completely, setup always runs. For partial re-takes. |
| `bts.rec.distance` | `16` | Render distance in chunks. |
| `bts.rec.fov` | `70` | Field of view. |
| `bts.rec.exit` | `true` | Close the client when done. |
| `bts.rec.guiscale` | height / 270 (4 at 1080p, 3 at 720p) | GUI scale for `gui` and `nei` screens, so a machine GUI reads at video size. A command's own `scale <n>` wins. The quest book uses height / 360 (3 at 1080p), see Limits. |
| `bts.rec.quests` | `true` | Before it creates its world, copy the worktree's `questbook/DefaultQuests` (and `questbook/resources`) into the client's `config/betterquesting/`. Run `command /btsquests install` in the setup as well: BetterQuesting reads its defaults at start-up, so on the very first run after the copy only the install puts the chapter into the world. |
| `bts.autorig.trimcards` | `3` | Acceleration cards on the AE2 Eye rig's two trim buses (one card slot is the redstone card). |

## Script format

One command per line. `#` starts a comment. Commands before the first `scene` are the **setup**, which is never
recorded. Each `scene <name>` starts a scene, written to its own segment. Inside a scene the commands run in order.

### Places

A place is an anchor with an optional offset, or absolute coordinates:

- `eye`: the Eye T1 rig's centre (where the star sits). `eyectrl`: the Eye's controller block.
- `kerr`: the Kerr T1 rig's centre (the hole). `keectrl`: the KEE controller block.
- `placed`: the controller the last `place` or `kit` put down. `here`: the camera now.
- `eye+0,30,-80` is the anchor plus (dx, dy, dz). `1000.5,40,3990` is absolute.
- A **named point** a rig registers as it is built, or a name given with `place ... name <n>` / `kit ... name <n>`
  (then `<n>` is the controller block and `<n>.c` the machine's centre: the star or the hole).
  - The AE2 Eye rig (`build eyeae`, Rig C): `aeeye` (the star), `aeeyectrl`, `ae.core` (the ME controller),
    `ae.drive`, `ae.emitter` (the level emitter's lamp), `ae.stockhatch`, `ae.stockbus`, `ae.outbus`, `ae.outhatch`,
    `ae.trunk` (the ground trunk's middle), `ae.riserhe`, `ae.riserh` (half-way up each riser), `ae.trimhe`,
    `ae.trimh` (the fluid export buses), `ae.dirhe`, `ae.dirh` (their Directive Hatches), `ae.fieldhe` (the Helium
    Field hatch), `ae.lamphe`, `ae.lamph` (the trim lamps), `ae.trimhe.cam`, `ae.trimh.cam` (a good camera spot in
    front of each trim station).
  - The Kerr rig (`build kerr`): `k.core`, `k.drive`, `k.iface`, `k.emitter`, `k.boltbus`, `k.seedbus`, `k.boltdir`,
    `k.seeddir`, `k.boltlamp`, `k.seedlamp`, `k.trunk`.

The rigs face north (-z), so their fronts are on the -z side. Anchors exist once their `build` has run.

### Commands

| Command | Recorded | What it does |
|---|---|---|
| `scene <name>` | | Starts a scene (segment `NN_<name>.mp4`). |
| `caption <text>` | | The caption: a semi-transparent bar at the bottom with white text. It stays until the next `caption` in the same scene (so a walk-through can change it as the camera moves) or the scene's end, and fades in with the first recorded frame after it. Write it for someone who has never seen the mod: what they are looking at, in plain words. |
| `label <place> [side up\|down\|left\|right] \| <text>` | | A callout: a dot on the place (projected from the world's own camera every frame), a line, and a dark box with the text, set off to the side given (up-right by default). Stays until `label clear` or the scene's end; hidden while a GUI is open. |
| `fov <degrees>` | | Field of view from here on (a wide shot of a big structure). |
| `card <seconds> <title> \| <line> \| ...` | yes | Title or end card: dark background, centred text. The first line is large. The text fades in and out. |
| `hold <seconds> [speed <x>] [clock]` | yes | Records the camera where it is. With `speed` above 1 it is a time-lapse (below). |
| `orbit <centre> radius <r> [height <h>] [from <deg>] [to <deg>] seconds <s> [look <place>] [ease inout\|linear\|in\|out] [speed <x>] [clock]` | yes | Flies round the centre, looking at it (or at `look`). Angle 0 is north of the centre (-z) and 90 is east (+x). The camera is `h` above the centre. If the camera is not already at the start, a `goto` there runs first, unrecorded. `seconds` is video time; with `speed` the move takes `seconds x speed` of real time and is recorded as a time-lapse. |
| `dolly <from> <to> seconds <s> [look <place>] [ease ...] [speed <x>] [clock]` | yes | Moves in a straight line, looking at `look` the whole way, or keeping the current direction. A `goto` to `<from>` runs first if needed. |
| `pan <place> seconds <s> [speed <x>]` | yes | Turns the camera smoothly to look at the place. |
| `timelapse <video-seconds> [speed <x>] [until <condition>] [tail <video-seconds>] [clock] [sky]` | yes | **Fast-forward capture.** The game runs at its real rate; the recorder keeps one frame every `x` frame periods, so `video-seconds x speed` of real time become `video-seconds` of video (speed 30: 15 minutes become 30 s). A "TIME-LAPSE ×30" tag is drawn top right; with `clock` it also shows the real time elapsed since the time-lapse began (`+7:12`). With `until <condition>` (see `wait`) it ends once the condition holds, `tail` video seconds later; `video-seconds` is then the cap. `sky` turns the daylight cycle on for it. |
| `goto <place> [look <place>]` | | Teleports the camera (the server loads the chunks), then waits until the chunks have arrived and rendered. |
| `look <place>` | | Turns the camera to the place at once. |
| `gui <place> [scale <n>]` | | The server opens the GT machine GUI at that block for the player (the camera must be within about 8 blocks, so `goto` first). Waits until the screen is up. NEI's panels are hidden and the mouse is parked in the bottom-left corner (no hover tooltips). GUI scale: `-Dbts.rec.guiscale` (default 4 at 1080p). Follow it with `hold`, then `close`. |
| `questbook [chapter [<id>] \| quest <id> \| <id>] [scale <n>]` | | Opens BetterQuesting's book: as it opens, on a chapter (`chapter` alone is the addon's, `190:47990`), or one quest's page (`48000` = high 190 low 48000, or `high:low`). Follow with `hold` and `close`. |
| `nei usage\|recipe <item> [tab <text>] [scale <n>]` | | NEI's recipe screen for the item (`ItemList.<name>` or `bts.<field>`): `usage` (what it is used in; BlockRenderer6343's multiblock preview, with its Tier and Layer sliders, is a usage tab, `tab multiblock`) or `recipe` (how it is made). `tab` selects the first tab whose name or handler class contains the text; the tabs found are logged. Follow with `hold` and `close`. |
| `close` | | Closes the open screen (the world's GUI scale comes back). |
| `build eye\|kerr\|both` | | Builds the autoshots photo rigs (`AutoRig`): Rig A, the Eye T1 on redstone and GT covers, which lights itself about 200 s after the build; Rig B, the Kerr T1 on AE2 (orange dense cable: one straight run out from each station and a cross trunk in front of the KEE, the core on it). |
| `build eyeae` | | Builds Rig C (`AutoRigEyeAE`), an Eye T1 fed and held only through AE2, at x 2400. **Main network** (orange): an ME stocking input hatch (Helium, Hydrogen) and an ME stocking input bus (Stellar Ignition Charge) in the front gas sockets, the kit's ME output bus and hatch cabled in, a creative-energy ME controller, a drive and a level emitter on the charges, on a dense trunk in front of the Eye. **Trim network** (light blue, its own creative-energy controller): two dense risers to the band, where two Directive Hatches (Trims, Bit 1 and Bit 2) each switch an AE2FC fluid export bus (Helium, Hydrogen; a redstone card, "active with signal") into a small (LV) Field hatch through one redstone dust on the bus block's facade. Each network's gas comes from a pair of GT Debug Fluid Tanks behind read-only AE2FC fluid storage buses, a stand-in for the player's gas production. Why two networks and LV hatches: see Limits. The tanks are empty and the drive holds no cell until `aecells`. Per-second log `run/client/screenshots/autorig/eye-ae.tsv`; once a minute the server log describes each trim bus. |
| `aecells` | | Rig C's supply opens (set up by script): the four tanks get endless Helium and Hydrogen, and the drive a 64k cell with 8 Stellar Ignition Charges plus empty cells for the products. From then on nothing touches the Eye: it fills its buffers, takes a charge and lights itself (40-45 s in the tests) and the trims hold the field (76 % Resonant, 24 % Stable over 392 s in the test). |
| `cell` | | Puts the 64k cell into Rig B's ME drive, which starts the Kerr. |
| `kit eye <tier 1-6> <place> [gen <0-8>] [facing <dir>] [name <n>]` | | Raises a whole Eye by its dev kit (as `/btseye t<n> x y z`): the bands for the tier, the field generators at `gen` (compression raised to the tier's gate), every hatch, unlocked to the tier, formed, dark. Sets `placed` and the name. |
| `kit kerr t1\|t2 <place> [facing <dir>] [name <n>]` | | Raises a whole Kerr by its dev kit (as `/btskerr [t2] x y z`): the KEE with both modules, formed, the hole closed. |
| `place <machine> <place> [facing <dir>] [name <n>]` | | Places a GT machine's controller. `<machine>` is `ItemList.<name>` (GT, e.g. `ItemList.Machine_Multi_BlastFurnace`) or `bts.<field>` (this addon's `BTSMachines`, e.g. `bts.eyeOfHarmony`). Sets the `placed` anchor. |
| `hologram <place> hints [tier <n>]` | | Uses a Hologram Projector on the controller as the player: the structure's hint blocks appear. |
| `hologram <place> build [tier <n>]` | | Builds the multiblock the way a creative player's sneak-use of the projector does (all at once). |
| `hologram <place> survival [tier <n>] [budget <b>] [every <ticks>] [tail <s>] [max <s>] [speed <x>] [clock]` | yes | **Step-by-step build.** The projector's survival auto-place, recorded: one `survivalConstruct(projector of stack size tier, b, ...)` round every `every` ticks from an endless item source (as a creative player's projector draws on), so the structure goes up in the order a player's projector places it. Our multis scale the budget (the Eye T1 places about 8 elements per budget unit: budget 25 is the whole 19,616-block core in about 100 rounds, 5 s; budget 6 about 20 s). Ends `tail` video seconds after the last block, or at `max`. It places blocks only: a `kit` at the same place then fits the hatches and forms it. |
| `time day\|noon\|sunset\|night\|sunrise\|<ticks>` | | Sets the time of day. The daylight cycle is off, except during `timelapse ... sky`. |
| `wait <condition> [timeout <s>]` | | Waits until a server condition holds (default timeout 600 s): `eyelit` (Rig A's star burns), `holeopen` (Rig B's hole is open), `aelit` / `aedark` (Rig C's star), `formed` (the `placed` machine is formed). |
| `wait seconds <n>` | | Waits n real seconds. |
| `command <server command>` | | Runs a server command as the player (op), e.g. `command /btseye ...`. |

### Example

```
# setup
build both
time day

scene title
  card 3 Beyond the Stars | The Eye of Harmony and the Kerr engine

scene kerr-start
  caption The Kerr engine, tier 1, fed by an AE2 network: the cell goes in and the network sends the seed
  goto kerr+36,16,-58 look kerr+0,-6,-4
  hold 2
  cell
  timelapse 6 speed 10

scene eye-orbit
  caption The Eye of Harmony, tier 1: a star held in a magnetic field, kept stable by plain redstone
  wait eyelit timeout 900
  orbit eye+0,-6,-10 radius 75 height 28 from -40 to 40 seconds 14

scene eye-gui
  caption The Eye's controller screen: the star's state and the field gauges
  goto eyectrl+0,0.2,-3.5 look eyectrl
  gui eyectrl
  hold 4
  close

scene end
  card 3 Thanks for watching
```

## Limits

- Dev client only, in an integrated world it creates. It does not attach to an existing save or a dedicated server.
- `gui` opens GT machine GUIs only (anything whose tile is a GT `CommonMetaTileEntity`); the Eye's ModularUI screen
  was checked on video.
- The quest book: the recorder copies the worktree's `questbook/DefaultQuests` into the dev run (`bts.rec.quests`) and
  the script runs `command /btsquests install`; the chapter "Beyond the Stars" and its quests then show. On this
  branch the overlay is that one chapter (34 quests) plus the "Unit One" entry quest, which belongs in the pack's
  *How to Generate Power* chapter; the dev run has no pack quests, so `/btsquests` reports that entry as missing.
  BetterQuesting draws its book smaller than the frame and in the top-left corner when the GUI scale leaves the
  screen under about 288 GUI units high (scale 4 at 1080p is 270), so `questbook` defaults to height / 360 (3 at
  1080p), where the book fills the frame. `questbook <id>` for a quest that does not exist opens nothing (10 s
  timeout, logged).
- `hologram ... hints` shows StructureLib's hint blocks; they fade after StructureLib's hint lifetime, so
  record them soon after the command.
- A time-lapse speeds up the video, not the game: the game always runs at its real rate, so a process that takes
  15 minutes in game still takes 15 minutes to record (`timelapse 30 speed 30` films it as 30 s of video).
- **Rig C, what the build taught.** (1) An ME stocking hatch in an Eye gas socket shows the Eye up to 2.1 G L of
  each fluid a second, and the Eye's buffers (cap 2^62 L) take all of it: a network of filled fluid cells was emptied
  into the Eye within seconds, and trim buses on the same network then found nothing (the pressure sat at +35). So the
  trims have their own network. (2) A trim bus faster than the Eye's trim draw (10,000 L/s for Sol) stockpiles trim in
  a big Field hatch, and the Eye keeps draining it after the bit goes off (both trims ran at once); a small LV hatch
  (16,000 L) caps that at about a second. Both are worth a line in the Eye's AE2 docs on master.
- `nei` opens the screen and selects a tab; it does not move BlockRenderer6343's Tier slider (the preview shows the
  tier the slider starts on, 1).
- The window must stay unminimised while recording.
