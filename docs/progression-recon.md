# PROGRESSION RECON — 0.22.0 spec (recon unit; no production code)

Scope: answer the six questions in the 0.22.0 brief with **real** references, then hand the
implementing units a spec they can follow without guessing.

Everything about the Forge of the Gods below was read out of the **actual sources at the pinned
tag**, not from memory:

- Jar on the compile classpath:
  `~/.gradle/caches/modules-2/files-2.1/com.github.GTNewHorizons/GT5-Unofficial/5.09.52.594/1e262bb361ec48b7b25af07c4b43253209d01fc5/GT5-Unofficial-5.09.52.594-dev.jar`
  (no `-sources` jar exists in the cache; the MCP fernflower at
  `.gradle/caches/retro_futura_gradle/mcp/fernflower.jar` predates lambdas and fails on
  `ForgeOfGodsUpgrade`, `UpgradeStorage`, `UpgradeTreePanel`, `MilestonePanel`).
- So the sources were fetched from GitHub **at the exact pinned tag `5.09.52.594`** and cross-checked
  against the jar's own class list. Verification: the class list under
  `gregtech/common/gui/modularui/multiblock/godforge/**` in the jar matches the tag's file list
  one-for-one, and the fernflower output for `Milestones.class` and `GodforgeMath.class` (which did
  decompile) matches the tag's `.java` for those two exactly.

Two packages hold the whole system:

| Package | What lives there |
|---|---|
| `tectech.thing.metaTileEntity.multi.godforge` | the MTE, the data object, the upgrade enum, the math |
| `gregtech.common.gui.modularui.multiblock.godforge` | every ModularUI2 panel, the sync plumbing, the milestone metadata |

---

## 1. MILESTONES — what the Godforge actually does

### 1.1 The four lines

`gregtech/common/gui/modularui/multiblock/godforge/data/Milestones.java` — a 4-constant enum. Each
constant carries its textures, its screen corner, its two lang keys, and **four sync-value handles**
(total / level / progress / inverted-progress):

| Constant | Counter it displays | Corner | Bar |
|---|---|---|---|
| `CHARGE` | `SyncValues.TOTAL_POWER_CONSUMED` (`BigInteger` of EU) | `Alignment.TopLeft` | red |
| `CONVERSION` | `SyncValues.TOTAL_RECIPES_PROCESSED` (long) | `TopRight` | purple |
| `CATALYST` | `SyncValues.TOTAL_FUEL_CONSUMED` (long) | `BottomLeft` | blue |
| `COMPOSITION` | `SyncValues.MILESTONE_COMPOSITION_LEVEL` (extensions built) | `BottomRight` | rainbow |

**Seven tiers each.** The `/ 7.0f` divisor is hard-coded in every `determine*Milestone` method in
`GodforgeMath.java`, and `IndividualMilestonePanel.getLevel()` clamps with `Math.min(rawLevel, 7)`
unless "inversion" (the post-completion endless mode) is on.

### 1.2 How progress is accumulated

`GodforgeMath.queryMilestoneStats(module, data)` — called once every 5 seconds per module from
`MTEForgeOfGods.onPostTick` (`MTEForgeOfGods.java:448`):

```java
data.setTotalPowerConsumed(data.getTotalPowerConsumed().add(module.getPowerTally()));
module.setPowerTally(BigInteger.ZERO);
data.setTotalRecipesProcessed(data.getTotalRecipesProcessed() + module.getRecipeTally());
module.setRecipeTally(0L);
```

So: **modules tally locally, the controller drains the tallies into its own running totals.** The
totals live on the controller, never on a module. That is exactly the shape our plant needs.

### 1.3 How a tier threshold is expressed

Not a table — a **logarithm**. `ForgeOfGodsData.java:27-35`:

```java
public static final long   POWER_MILESTONE_CONSTANT  = LongMath.pow(10L, 15);   // 1e15 EU
public static final long   RECIPE_MILESTONE_CONSTANT = LongMath.pow(10L, 7);    // 1e7 recipes
public static final long   FUEL_MILESTONE_CONSTANT   = 10_000L;
public static final double POWER_LOG_CONSTANT  = Math.log(9.0D);
public static final double RECIPE_LOG_CONSTANT = Math.log(4.0D);
public static final double FUEL_LOG_CONSTANT   = Math.log(3.0D);
```

and `GodforgeMath.determineChargeMilestone`:

```java
rawProgress = (float) Math.max(
        Math.log(total.divide(BigInteger.valueOf(POWER_MILESTONE_CONSTANT)).longValue())
            / POWER_LOG_CONSTANT + 1.0D, 0.0D) / 7.0F;
data.setPowerMilestonePercentage(rawProgress);
data.setMilestoneProgress(0, (int) Math.floor(rawProgress * 7.0F));
```

i.e. **`level = floor( log(total / BASE) / log(R) + 1 )`**, so threshold(n) = `BASE * R^(n-1)`:

| Line | BASE | R | Tier 7 threshold |
|---|---|---|---|
| CHARGE | 1e15 EU | 9 | 9^6 x 1e15 = **5.31e20 EU** |
| CONVERSION | 1e7 recipes | 4 | 4^6 x 1e7 = **4.096e10 recipes** |
| CATALYST | 1e4 fuel | 3 | 3^6 x 1e4 = **7.29e6 fuel** |
| COMPOSITION | 1 extension | linear | 7 (`level = floor(totalExtensionsBuilt)`) |

`IndividualMilestonePanel.getLevelProgress()` re-derives the *next* threshold with the same
constants (`LongMath.pow(9, level) * LongMath.pow(10, 15)` etc.).

### 1.4 What a tier grants

**Nothing directly.** A tier grants **graviton shards**, and shards buy nodes in the upgrade tree.
`MTEForgeOfGods.determineGravitonShardAmount()` (`:964`):

```java
int sum = 0;
for (int progress : data.getAllMilestoneProgress()) {
    if (!data.isInversion()) progress = Math.min(progress, 7);
    sum += progress * (progress + 1) / 2;         // triangular
}
data.setGravitonShardsAvailable(sum - data.getGravitonShardsSpent());
```

Triangular award: 7 tiers = 28 shards per line, **4 lines x 28 = 112 shards**. And the sum of every
`cost(...)` in `ForgeOfGodsUpgrade` is **exactly 112** (added by hand: 6x1 + 5x2 + 6x3 + 2x4 + 4 + 5 +
6 + 7 + 8 + 9 + 10 + 12 = 112). The debug shard field is even bounded `numbersInt(0, 112)`
(`UpgradeTreePanel.java:296`). **A full milestone sweep buys exactly the whole tree — no waste, no
shortfall.** That property is worth copying.

### 1.5 How progress is displayed

Two panels, both ModularUI2, reached by a button — not a tab strip:

- `panel/MilestonePanel.java` — a 400x300 panel on `GTGuiTextures.BACKGROUND_SPACE`, with one
  `ParentWidget` per milestone pinned to a corner via `topRel/leftRel/bottomRel/rightRel`. Each
  parent holds a `ButtonWidget` whose background is the milestone art (click -> detail panel),
  **two `ProgressWidget`s** (forward + inverted, `Direction.RIGHT` / `Direction.LEFT`, each fed a
  `DoubleSyncValue` wrapping a `FloatSyncValue`), and an `IKey.lang(...)` title.
- `panel/IndividualMilestonePanel.java` — a 150x150 panel with four `IKey.dynamic` lines: total
  progress, current level, next threshold, shards gained; plus a number-formatter cycle button.

So: **bars + numbers on dedicated sub-panels.**

---

## 2. UPGRADE TREE — the data model

`tectech/thing/metaTileEntity/multi/godforge/upgrade/ForgeOfGodsUpgrade.java` (503 lines).

### 2.1 The node

An **enum with 31 constants** (`START`, `IGCC`, `STEM`, ... `EE`, `END`), each built inside a
`static {}` block by a fluent builder — done there, per its own comment, *"due to potential forward
references"* (`DOP` names `CNTI`, which is declared after it):

```java
QGPIU.build(b -> b
    .prereqs(REC, CTCDD)
    .cost(2)
    .background(BLUE, CATALYST)
    .treePos(126, 416));

REC.build(b -> b
    .prereqs(GISS, GPCI)
    .requireAllPrereqs()          // default is ANY-of
    .cost(2)
    .background(RED, CHARGE)
    .treePos(56, 356));
```

Per-node fields (`ForgeOfGodsUpgrade.java:280-305`):

| Field | Meaning |
|---|---|
| `ForgeOfGodsUpgrade[] prerequisites` | inbound edges |
| `boolean requireAllPrerequisites` | AND vs OR over those edges (**OR is the default**) |
| `int shardCost` | currency cost |
| `List<ItemStack> extraCost` | up to 12 stacks, added later by the recipe loader |
| `UpgradeColor color`, `Milestones icon` | which glow background / which symbol overlay |
| `PanelSize panelSize` | `STANDARD(250,80,115)` or `LARGE(300,55,170)` for the detail panel |
| `int treeX, treeY` | **absolute pixel position inside the scrolling tree panel** |
| `ForgeOfGodsUpgrade[] dependents` | **derived** — the `static {}` block inverts the prereq map |
| 4 lang keys | `fog.upgrade.tt.<ordinal>`, `.short.<ordinal>`, `fog.upgrade.text.<ordinal>`, `.lore.<ordinal>` |

The **effect is not on the node.** Effects are read back out by `GodforgeMath` with
`data.isUpgradeActive(ForgeOfGodsUpgrade.X)` at the point of use — 30-odd such branches across
`calculateMaxParallelForModules`, `calculateMaxHeatForModules`, `calculateEnergyDiscountForModules`,
`setMiscModuleParameters`, `allowModuleConnection`. Example:

```java
if (data.isUpgradeActive(ForgeOfGodsUpgrade.SA)) {
    fuelFactorMultiplier = 1.0F + calculateEffectiveFuelFactor(fuelFactor) / 15.0F;
    if (data.isUpgradeActive(ForgeOfGodsUpgrade.TCT)) { ... }
}
```

That is a **deliberate choice worth copying**: the node knows nothing about the machine; the machine
asks "is node X on?" at the point the number is computed. It keeps the graph declarative and makes
every effect greppable.

### 2.2 Unlock state and persistence

`upgrade/UpgradeStorage.java` — one `EnumMap<ForgeOfGodsUpgrade, UpgradeData>`, where `UpgradeData` is
`{boolean active; boolean costPaid; short[12] amountsPaid;}`.

```java
public void serializeToNBT(NBTTagCompound NBT, boolean force) {
    if (!force && !hasAnyProgress()) return;
    NBTTagCompound upgradeTag = new NBTTagCompound();
    for (ForgeOfGodsUpgrade upgrade : ForgeOfGodsUpgrade.VALUES) {
        upgradeTag.setBoolean("upgrade" + upgrade.ordinal(), data.isActive());
        ...
    }
    NBT.setTag("upgrades", upgradeTag);
}
```

**Keyed by `ordinal()`.** That is the one thing here I recommend *not* copying (see 6.3).

There is also a `GenericListSyncHandler<UpgradeData>` (`:228-239`) that syncs the whole tree to the
client with a hand-written `writeToBuffer`/`readFromBuffer` pair.

### 2.3 Buying and refunding

`ForgeOfGodsData.java:358-380`. Note that it **does** re-validate server side — the client panel's
checks only grey out buttons:

```java
public void unlockUpgrade(ForgeOfGodsUpgrade upgrade) {
    if (isUpgradeActive(upgrade)) return;
    if (!upgrades.checkPrerequisites(upgrade)) return;
    if (!upgrades.checkSplit(upgrade, ringAmount)) return;
    if (!upgrades.checkCost(upgrade, gravitonShardsAvailable)) return;
    upgrades.unlockUpgrade(upgrade);
    gravitonShardsAvailable -= upgrade.getShardCost();
    gravitonShardsSpent     += upgrade.getShardCost();
}

public void respecUpgrade(ForgeOfGodsUpgrade upgrade) {
    if (!isUpgradeActive(upgrade)) return;
    if (!upgrades.checkDependents(upgrade)) return;      // guards orphaning
    upgrades.respecUpgrade(upgrade);
    gravitonShardsAvailable += upgrade.getShardCost();
    gravitonShardsSpent     -= upgrade.getShardCost();
}
```

**Yes, nodes are refundable** — free, unlimited, right-click. `UpgradeStorage.checkDependents`
(`:125-142`) is the interesting bit: a node may only be refunded if every active dependent either
does not strictly require it *or* still has >= 2 active prerequisites left.

Currency is tracked as **`available` and `spent`**, and `available` is *recomputed from scratch* every
5 s as `sum(triangular tiers) - spent`, so the earned total can grow without the spend bookkeeping
drifting.

`resetAll()` / `unlockAll()` exist behind `ConfigHandler.debug.DEBUG_MODE`, wired to two buttons in
`UpgradeTreePanel.getDebugWidgets` (`:271-320`) — precedent for the debug command the brief asks for.

---

## 3. THE GUI — the part most likely to defeat a blind implementation

### 3.1 Which ModularUI this addon has — **MUI2 2.3.70, confirmed**

- `addon/dependencies.gradle` pins only `GT5-Unofficial:5.09.52.594:dev` and `StructureLib:1.4.38:dev`.
- `GT5-Unofficial-5.09.52.594.pom` declares **`ModularUI2` version `2.3.70-1.7.10`, classifier `dev`,
  scope `compile`** (plus `ModularUI` 1.3.4 for legacy MUI1). `compile` scope = transitively visible
  to this addon at compile time. (A `2.3.85-1.7.10` jar is also in the cache — it belongs to
  `GT5-Unofficial:5.09.54.93`, which this addon does **not** depend on. Do not code against it.)
- `MTEReactorSiloGui.java` already imports `com.cleanroommc.modularui.*` and extends
  **`gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui<MTEReactorCoreMk1>`** — the
  same base the Godforge GUI family sits beside.

**So the Godforge's rendering approach is reachable.** Verified present in the 2.3.70 jar by `javap`:
`ScrollWidget`, `AbstractScrollWidget`, `VerticalScrollData` (with `setScrollSize(int)`),
`HorizontalScrollData`, `ButtonWidget`, `ProgressWidget`, `DynamicDrawable`, `ParentWidget`, `Flow`,
`IPanelHandler`, `EnumSyncValue`, `GenericListSyncHandler`, `PanelSyncManager.syncedPanel(...)`.

Verified present and `public` in the GT jar:
`gregtech.api.modularui2.GTGuiTextures` — `BACKGROUND_STAR`, `BACKGROUND_SPACE`,
`BACKGROUND_GLOW_{BLUE,RED,GREEN,ORANGE,PURPLE,WHITE,RAINBOW}`, `BUTTON_SPACE_32x16`,
`BUTTON_SPACE_PRESSED_32x16`, `PICTURE_UPGRADE_CONNECTOR_{BLUE,RED,GREEN,ORANGE,PURPLE}[_OPAQUE]`,
`PROGRESSBAR_GODFORGE_MILESTONE_{BACKGROUND,RED,BLUE,PURPLE,RAINBOW}[_INVERTED]`,
`PICTURE_GODFORGE_MILESTONE_{CHARGE,CONVERSION,CATALYST,COMPOSITION}[_GLOW]`;
`gregtech.common.gui.modularui.widget.RotatedDrawable` (`rotationRadian(float)`);
`gregtech.common.gui.modularui.multiblock.godforge.data.UpgradeColor`;
`gregtech.common.gui.modularui.multiblock.godforge.ForgeOfGodsGuiUtil.panelCloseButton()`.

### 3.2 What is NOT reachable

`gregtech.common.gui.modularui.multiblock.godforge.sync.SyncHypervisor` is `public final`, but every
useful method is typed to TecTech:

```
public tectech.thing.metaTileEntity.multi.godforge.MTEForgeOfGods getMultiblock();
public tectech.thing.metaTileEntity.multi.godforge.util.ForgeOfGodsData getData();
public <T extends tectech...MTEBaseModule> T getModule(Modules<T>);
```

Same for `SyncValues` (every constant is a `ForgeOfGodsSyncValue` bound to `ForgeOfGodsData`),
`Panels`, `SyncActions`, and every `*Panel` class (they all take a `SyncHypervisor`).

**Verdict, plainly: the Godforge's widgets and textures are 100% reachable from this addon; its sync
plumbing is not, and must not be attempted.** The good news is that we do not need it —
`SyncHypervisor` exists only because the Godforge has *five* MTEs (controller + four module types)
sharing one GUI and needs to route sync values between them. We have one controller. The plain
`PanelSyncManager` + `syncManager.syncedPanel(...)` route that `MTEMultiBlockBaseGui` already uses
covers us completely.

### 3.3 How the node graph is actually rendered

`panel/UpgradeTreePanel.java` — the whole trick, and it is much simpler than it looks:

```java
private static final int SIZE = 300, OFFSET_SIZE = 292, SCROLL_SIZE = 957;
private static final int BUTTON_W = 40, BUTTON_H = 15;

ModularPanel panel = ...;
panel.size(SIZE).padding(4,0,4,0).background(GTGuiTextures.BACKGROUND_STAR)
     .disableHoverBackground().child(ForgeOfGodsGuiUtil.panelCloseButton());

// "ListWidget doesn't work here due to how the tree is built"
VerticalScrollData scrollData = new VerticalScrollData();
scrollData.setScrollSize(SCROLL_SIZE);                 // 957px of virtual canvas
ScrollWidget<?> treeList = new ScrollWidget<>(scrollData).size(OFFSET_SIZE);
```

**Node positions**: every node is a `ButtonWidget` at `.pos(upgrade.getTreeX(), upgrade.getTreeY())`,
`.size(40, 15)`, added as a child of the `ScrollWidget`. Absolute pixels inside the 292 x 957 canvas.
There is no layout engine involved at all.

**Node appearance** — note `disableThemeBackground(true)`; the *overlay* carries the whole look,
because the connector lines otherwise draw over a background layer no matter the child order (the
class says so in a comment at `:135-136`):

```java
widget.size(BUTTON_W, BUTTON_H).pos(x, y)
  .disableThemeBackground(true).disableHoverThemeBackground(true)
  .overlay(new DynamicDrawable(() -> data.isUpgradeActive(upgrade)
              ? GTGuiTextures.BUTTON_SPACE_PRESSED_32x16
              : GTGuiTextures.BUTTON_SPACE_32x16),
           IKey.lang(upgrade.getShortNameKey()).style(GOLD).scale(0.8f).alignment(Alignment.CENTER))
```

**Connecting lines** — the clever part. There is no line primitive. A 6px-wide connector texture is
stretched to the *distance* between two node centres and rotated to the *angle* between them
(`UpgradeTreePanel.java:190-223`):

```java
int fromCX = from.getTreeX() + BUTTON_W/2, fromCY = from.getTreeY() + BUTTON_H/2;
int toCX   = to.getTreeX()   + BUTTON_W/2, toCY   = to.getTreeY()   + BUTTON_H/2;
int width  = 6;
int height = (int) Math.sqrt(Math.pow(toCX-fromCX,2) + Math.pow(toCY-fromCY,2));   // length
float rotation = (float) (Math.atan2(toCY-fromCY, toCX-fromCX) - Math.PI/2);        // angle
int x = (fromCX+toCX)/2 - width/2;                                                  // midpoint,
int y = (fromCY+toCY)/2 - height/2;                                                 // then top-left

return new DynamicDrawable(() -> {
        UITexture tex = color.getConnector();
        if (data.isUpgradeActive(from) && data.isUpgradeActive(to)) tex = color.getOpaqueConnector();
        return new RotatedDrawable(tex).rotationRadian(rotation);
    }).asWidget().pos(x, y).size(width, height);
```

Lines are added to the `ScrollWidget` **before** the buttons, and they are declared by hand — a
second, hand-maintained list of 37 `createConnectorLine(colour, from, to, ...)` calls that duplicates
the `prereqs(...)` graph and can silently disagree with it.

**Clicking**: `.onMousePressed(d -> ...)`, `d == 0` left, `d == 1` right.

- left: set an `EnumSyncValue<ForgeOfGodsUpgrade>` to the clicked node, open the detail panel.
- shift+left (`Interactable.hasShiftDown()`): buy outright (`SyncActions.COMPLETE_UPGRADE`), or open
  the item-cost panel if the item cost is unpaid.
- right: `SyncActions.RESPEC_UPGRADE`.

**Scroll/pan**: **vertical only.** `ScrollWidget`'s public constructors take *either* a
`VerticalScrollData` *or* a `HorizontalScrollData`; only `AbstractScrollWidget(Horizontal, Vertical)`
gives both, so two-axis panning needs a three-line subclass.
**Recommendation: don't. Design the tree 292px wide and scroll vertically only**, exactly as the
Godforge does. It is the known-good geometry.

### 3.4 The achievable design for this addon (what to build)

Fully compilable against what we already have; no GT-internal API beyond public textures.

```java
// in MTEReactorSiloGui
IPanelHandler milestonePanel = syncManager.syncedPanel("btsMilestones", true,
        (psm, sh) -> BTSMilestonePanel.build(parent, psm, multiblock));
IPanelHandler treePanel      = syncManager.syncedPanel("btsUpgradeTree", true,
        (psm, sh) -> BTSUpgradeTreePanel.build(parent, psm, multiblock));
```

Precedent for exactly this call: `MTEMultiBlockBaseGui.createPowerPanelButton` (`:1004-1021`) —
`syncManager.syncedPanel(name, true, builder)` then a `ButtonWidget` that toggles it. The two buttons
go in the addon's existing `createRightPanelGapRow(...)` override (`MTEReactorSiloGui.java:498`),
remembering that GT's gap row is `reverseLayout(true)` so **the last child added is leftmost**.

New files, all in `com.nateyburr.beyondthestars.progression`:

| File | Role |
|---|---|
| `BTSMilestone.java` | 4-constant enum: counter accessor, unit, 7-entry `long[] thresholds`, art, lang keys |
| `BTSUpgrade.java` | 25-constant enum + `static {}` builder block: `prereqs / cost / colour / icon / treePos` |
| `BTSProgression.java` | the data object — counters, node set, points; `writeNBT`/`readNBT`; all validation |
| `BTSMilestonePanel.java` | 400x300, four corner tiles, two `ProgressWidget`s each |
| `BTSMilestoneDetailPanel.java` | 150x150, four `IKey.dynamic` lines |
| `BTSUpgradeTreePanel.java` | 300-wide `ScrollWidget`; connector lines **derived from the prereq edges** |
| `BTSUpgradeDetailPanel.java` | node name/body/lore + BUY / REFUND |
| `CommandBTSProgress.java` | the debug setter (section 6.6) |

Sync, in the addon's existing idiom (`MTEReactorSiloGui.registerSyncValues`, `:44-158`):

```java
syncManager.syncValue("progUpTicks",  new LongSyncValue(multiblock::getProgUpTicks));
syncManager.syncValue("progBurnup",   new LongSyncValue(multiblock::getProgBurnup));
syncManager.syncValue("progReproc",   new LongSyncValue(multiblock::getProgReproc));
syncManager.syncValue("progPerfect",  new LongSyncValue(multiblock::getProgPerfect));
syncManager.syncValue("progTierMask", new IntSyncValue(multiblock::getProgTierMask));   // 4 nibbles
syncManager.syncValue("progPoints",   new IntSyncValue(multiblock::getProgPointsAvailable));
syncManager.syncValue("progNodeMask", new LongSyncValue(multiblock::getProgNodeMask));  // 25 nodes
syncManager.syncValue("progBuy",      new IntSyncValue(() -> -1, multiblock::progBuyNode).allowC2S());
syncManager.syncValue("progRefund",   new IntSyncValue(() -> -1, multiblock::progRefundNode).allowC2S());
```

A 25-node set fits in one `long`; four tiers of 0..7 fit in one `int` as four nibbles. **Two sync
values instead of thirty** — far less to get wrong than the Godforge's per-node
`GenericListSyncHandler`, and it makes the tree panel's `DynamicDrawable` suppliers pure reads of a
single synced long.

**AXIS 1 warning for the implementers.** Every counter is written *server* side. The panels must read
the sync handlers via `syncManager.findSyncHandler(...)`, never `multiblock.getProgXxx()` from inside
a widget supplier without a sync value behind it — a direct client read is 0 and the tree would
render every node locked on a fully upgraded plant. This is the exact bug class the file already
documents at `MTEReactorSiloGui.java:57-63`.

---

## 4. THE PLANT'S OWN NUMBERS

### 4.1 The EU/t formula, term by term

Read out of `MTEReactorCoreMk1.java`. Exactly two call sites derive it: `onRunningTick` (`:3953`) and
`getLiveEUDemand()` (`:5997`). Everything else is a formatter fed the synced result.

| # | Term | Where | Value on my maxed T5 |
|---|---|---|---|
| 1 | `STEAM_PER_COLUMN` | `:137` | 16,384 L/t per pillar |
| 2 | `ROD_MULT[grade]` | `:167` | `{0,1,2,5,9}` — naq-bonded = **x9** |
| 3 | `steamBase = SUM over live pillars(1 x 2)` | `:3815` | 201 x 16,384 x 9 = **29,642,496 L/t** |
| 4 | throttle `n / 24`, n in 1..24 | `:3817`, setter `:6005` | n=24 -> **x1.000** |
| 5 | `spineSpeedMilli` | `:3097` | all-riding CW x4: 1.00 x 1.08 x 1.16 x 1.24 = **1.553** |
| 6 | `spineEffMilli` | `:3098` | all-riding -> **1.000** (efficiency is the *other* build) |
| 7 | `spineBoostedSteam(true)` = 3 x 4 x 5 x 6 | `:3987` | 29,642,496 x 1.553 = **46,034,795 L/t** |
| 8 | `rpmMilli / 500_000` | `:4008` | redline 12,000 RPM -> **x24.0** |
| 9 | `avgRotorFactorMilli / 1000` | `:4813` | 8 fresh rotors -> **x1.000** (each wear point -5%) |
| 10 | `demand = 7 x 8 x 9` | `:4008` | **1,104,835,080 EU/t** — the code's own "~1.10B peak" note |
| 11 | `outputCeiling() = dynamoCap + laserCap`, `min(..., Integer.MAX_VALUE)` | `:4065` | the fitted hatches |
| 12 | `gridCeiling()` — 100,000,000 with no Switchyard, else `Long.MAX_VALUE` | `:4084`, `:4101` | |
| 13 | `emitted = min(10, min(11, 12))` | `:3964` | |

Feeding the RPM term (`:4536-4546` gain, `:5016` decay, `:290` redline):
`gainMilli = RPM_GAIN(176) x P_milli x timed/8` per cycle, decay `x995/1000` per tick. At P = 6.5 and
8/8 timed on a 20-tick clock: gain 1,144 RPM/cycle vs `1 - 0.995^20 = 9.54%` lost per cycle ->
equilibrium ~11,992 RPM. **The design pins itself at the redline, which is why the redline is the
single most valuable thing an unlock can raise.**

Other terms a multiplier could attach to: feedwater draw `baseFlow >> coolingTowerCount()`
(`:3936-3939`, `:612`); chamber coolant drain `CHAMBER_COOLANT_Q` (`:4148`); heat accrual and
`SCRAM_HEAT 8000` / `MELTDOWN_HEAT 10000` (`:138-139`); rotor-wear roll
`chance = (rpm/redline)/600` per second (`:4825`); decay cycle length
`genICampaignMinutes*60*20/8` (`:3795`); `DERRICK_CYCLE_TICKS 200` and `ANNEX_CYCLE_TICKS 600`
(`:5171`, `:5183`); the assembly-fallback `/2` penalty (`:3874`, `:3892`, `:3919`).

### 4.2 Capabilities an unlock could gate — and what I ALREADY HAS

| Capability | Ceiling today | Already in my hands? |
|---|---|---|
| Build Tier | `MAX_TIER = 5` (`:153`), `COLUMN_CAP {0,45,77,109,157,201}` (`:176`) | **YES — tier 5** |
| Rod grade | `ROD_MULT` up to naq-bonded x9 (`:167`) | **YES** |
| Propellant | `PROPELLANT_RUNGS = 9`; rung 8 = Naq Propellant Lv 6, P = 6.5 (`:3564`, `:3572`) | **YES — Lv 6** |
| Coolant | 6 rungs, best = Tachyon Rich Temporal Fluid (`:4148`) | **YES** |
| Laser output | `mExoticDynamoHatches` summed into `outputCeiling()` (`:4065`) | **YES** |
| Grid soft-cap relief | Switchyard, or `Config.switchyardSoftCap = false` (`:4084`) | exists as a build |
| Throttle | hard-clamped 1..24 (`:6006`) | at max |
| Spine housings | 4 sockets | at max |
| Rotor stages | `GALLERY_STAGES = 8` (`:900`) | at max |
| Cooling towers | 2 (`:612`) | at max |
| RPM redline | `RPM_REDLINE = 12_000` (`:290`) | at max |

**Ruling for the implementers — and the brief's "say plainly which you chose":**

> **ADDITIVE ONLY. No milestone tier and no tree node may gate anything in the table above.** Every
> unlock opens a *new* rung strictly above today's ceiling: Build Tier **6/7**, propellant rung
> **9**, throttle **25..30**, redline **13,500 / 15,000**, a **9th** rotor stage, a **3rd** cooling
> tower, a softer assembly-fallback penalty. Nothing that stands today changes behaviour on the tick
> 0.22.0 loads.

This is the "grandfather" outcome achieved *structurally* rather than with a flag: because every gate
is a new maximum, a plant with progress = 0 keeps exactly the ceiling it has now, so there is nothing
to grandfather and nothing to get wrong. The alternative — gate Build Tier 5 and grandfather anyone
already at 5 — needs a "was-already-5" boolean in NBT, which is precisely the `btsLastBuiltTier`
shape M2 already identified as a regression trap (`MTEReactorCoreMk1.java:5652-5655` and the
deliberate non-read at `:5738`). Do not go back there.

The two places that must change to raise a ceiling:

- `setBuildTierSetting(int)` (`:6013`) clamps to `MAX_TIER`. Raise `MAX_TIER` to 7, extend
  `COLUMN_CAP` to `{0,45,77,109,157,201,253,313}`, register the two extra shapes **unconditionally**,
  and clamp *the setter* to `Math.min(progMaxBuildTier(), MAX_TIER)`.
  **AXIS 1:** do it in the setter, not in the shape registry — `construct(stack, hintsOnly=true)`
  runs client-side and must never need progression state to decide which shape exists.
- `setThrottleSetting(int)` (`:6005`) clamps to 24 -> `Math.min(progMaxThrottle(), 30)`.
  **The `TextFieldWidget.numbersInt(1, 24)` in `createRightPanelGapRow` (`:500-506`) must widen
  too**, or the field will refuse a value the server would accept.

---

## 5. THE FOUR MILESTONE LINES (proposal)

`PROPOSAL.md:100` names them: *uptime / burnup / reprocessing / turbine optimization*. Filled in:

### 5.1 The counters

| Line | Field (long) | Incremented where | Unit |
|---|---|---|---|
| **UPTIME** | `progUpTicks` | `onRunningTick` at `:3966`, `if (emitted > 0) progUpTicks++` | ticks (shown as hours) |
| **BURNUP** | `progBurnup` | `checkProcessing` cascade commit, the `burnKeys` loop at `:3846`, `+= ROD_MULT[type]` per segment burned | fuel-weighted segments |
| **REPROCESSING** | `progReproc` | `runDerrickTick` on deposit (`:5368`) and `runAnnexTick` on deposit (`:5447`), `++` each | loads |
| **TURBINE OPT.** | `progPerfect` | `evaluateCycle` (`:4544`), `if (hadSteam && good == 8) progPerfect++` | perfect cycles |

Deliberate: **burnup is weighted by `ROD_MULT`**, so the line rewards the fuel chain (uranium x1 ->
naq-bonded x9) rather than raw rod count. **Uptime counts only generating ticks**, so an idle
chunk-loaded plant earns nothing. **Turbine counts only 8/8-timed cycles that had a steam slug**, so
it is the "did you actually tune it" line and cannot be farmed by a fast clock with no fuel.

### 5.2 Thresholds — explicit tables, not logarithms

**Recommendation: `long[7]` tables, not the Godforge's `log()` formula.** The formula only earns its
keep because the Godforge needs unbounded post-tier-7 "inversion" levels; we do not. A table is
exact, has no float edge case at total = 0 (the Godforge needs a `Math.max(..., 0)` guard for exactly
that), is trivially unit-testable, and lets the tiers be readable round numbers.
`tier = count(thresholds <= counter)`, capped at 7.

Reference rates for **my maxed T5 plant** (201 naq pillars, all-riding spine, redline, 8
fresh rotors, default `genICampaignMinutes = 720`):

- decay cycle = 720 x 60 x 20 / 8 = **108,000 ticks = 90 min** (`:3795`)
- burnup = 201 x 9 = 1,809 units per decay cycle = **1,206 units/h**
- turbine = one cycle per `CYCLE_SLUG_TICKS = 20` (`:305`) = 3/s = **10,800 perfect cycles/h**
- reprocessing = Derrick 200 t (10 s) -> 360/h plus Annex 600 t (30 s) -> 120/h = 480/h theoretical;
  **assume 300/h sustained** once feedstock and bus-emptying are automated
- uptime = **72,000 ticks/h**

| Tier | UPTIME (ticks) | = | BURNUP (units) | = | REPROC (loads) | = | TURBINE (cycles) | = |
|---|---|---|---|---|---|---|---|---|
| 1 | 72,000 | 1 h | 2,000 | 1.7 h | 250 | 0.8 h | 5,000 | 0.5 h |
| 2 | 144,000 | 2 h | 5,000 | 4.1 h | 700 | 2.3 h | 15,000 | 1.4 h |
| 3 | 288,000 | 4 h | 13,000 | 10.8 h | 1,900 | 6.3 h | 45,000 | 4.2 h |
| 4 | 576,000 | 8 h | 34,000 | 28 h | 5,200 | 17 h | 135,000 | 12.5 h |
| 5 | 1,152,000 | 16 h | 88,000 | 73 h (3.0 d) | 14,000 | 47 h (2.0 d) | 400,000 | 37 h (1.5 d) |
| 6 | 2,304,000 | 32 h | 230,000 | 191 h (8.0 d) | 38,000 | 127 h (5.3 d) | 1,200,000 | 111 h (4.6 d) |
| 7 | 4,608,000 | 64 h (2.7 d) | 600,000 | 498 h (20.7 d) | 100,000 | 333 h (13.9 d) | 3,600,000 | 333 h (13.9 d) |

Arithmetic behind the "=" columns: `hours = threshold / rate`, with the rates above. E.g. burnup tier
7: `600,000 / 1,206 = 497.5 h`. Turbine tier 7: `3,600,000 / 10,800 = 333.3 h`. Reproc tier 7:
`100,000 / 300 = 333.3 h`. Uptime tier 7: `4,608,000 / 72,000 = 64 h`.

**The pacing story.** Uptime is the only line the player cannot accelerate by engineering, so it gets
the shallowest ladder (x2 per tier) and finishes in under three days of *generating* time — it is the
"just keep it lit" line and it is what carries the player through the early multipliers. The other
three are throughput lines the player *can* attack (better fuel, better automation, a tighter
redstone clock), so they get steeper ladders (x2.6 to x3) and top out at two to three weeks of a
maxed plant. Result: **tier 1 on all four lines inside the first two hours** — the reward drip starts
immediately, which matters because progress starts at zero — **tier 4 everywhere inside a day and a
half**, and **all-7s at roughly three weeks** of a continuously running tier-5 plant.

A tier-1 starter (45 uranium pillars = 30 burnup units/h, no Derrick, no Annex) reaches burnup tier 3
in about 19 days and will never see tier 7. That is correct: the line is supposed to say "your fuel
chain is not finished".

### 5.3 What each tier grants

Per the ruling: **tiers 1-4 = multipliers into the existing EU formula; tiers 5-7 = unlocks.** Every
tier *additionally* pays points on the Godforge's triangular schedule (`tier*(tier+1)/2`, 28 per
line, **112 total**), which is the tree's currency.

All multipliers are **milli** (1000 = x1.000) and are **new factors initialised to 1000**. None of
them edits an existing constant, so a plant with no progress produces bit-identical numbers.

**UPTIME — "the plant runs itself"** (attaches to the *steam* term)

| Tier | Grant |
|---|---|
| 1 | `progSteamMilli` 1000 -> 1020 |
| 2 | -> 1050 |
| 3 | -> 1090 |
| 4 | -> 1150 (+15% steam) |
| 5 | **UNLOCK** 3rd Cooling Tower socket (feedwater `>> 3`) |
| 6 | **UNLOCK** Build Tier **6** (`COLUMN_CAP` 253) |
| 7 | **UNLOCK** Build Tier **7** (`COLUMN_CAP` 313) |

**BURNUP — "the fuel chain"** (attaches to the *rod* term)

| Tier | Grant |
|---|---|
| 1 | `progFuelMilli` -> 1030 (scales each pillar's `ROD_MULT` contribution) |
| 2 | -> 1070 |
| 3 | -> 1120 |
| 4 | -> 1200 (+20%) |
| 5 | **UNLOCK** throttle overdrive: setter cap 24 -> **27** (n/24 may now exceed 1.0) |
| 6 | **UNLOCK** throttle cap -> **30** (x1.25) |
| 7 | **UNLOCK** assembly-fallback penalty 50% -> 75% (`:3874`, `:3892`, `:3919`) |

**REPROCESSING — "the back end"** (attaches to the *modules and the site*)

| Tier | Grant |
|---|---|
| 1 | `progModuleMilli` -> 1100 (Derrick/Annex cycle ticks scaled by `1000/progModuleMilli`) |
| 2 | -> 1250 |
| 3 | -> 1500 |
| 4 | -> 2000 (both modules at double rate) |
| 5 | **UNLOCK** Derrick tantalum bonus every **3rd** cycle instead of 4th (`DERRICK_TANTALUM_EVERY`) |
| 6 | **UNLOCK** rotor wear halved: the `chance = (rpm/redline)/600` roll (`:4825`) becomes `/1200` |
| 7 | **UNLOCK** propellant rung **9** — a NEW fluid above Naq Lv 6, P = 8.0 |

**TURBINE OPTIMIZATION — "the fan"** (attaches to the *RPM/rotor* terms)

| Tier | Grant |
|---|---|
| 1 | `progRpmGainMilli` -> 1050 (scales `RPM_GAIN`: faster spin-up, same redline) |
| 2 | -> 1120 |
| 3 | `progRotorMilli` -> 1050 (scales `avgRotorFactorMilli`) |
| 4 | `progRotorMilli` -> 1120 |
| 5 | **UNLOCK** redline 12,000 -> **13,500 RPM** |
| 6 | **UNLOCK** redline -> **15,000 RPM** |
| 7 | **UNLOCK** a **9th** rotor stage (`GALLERY_STAGES` 8 -> 9; the averaging divisor at `:4813` moves with it) |

Sanity check on the top end. Uptime T4 (+15% steam) x burnup T4 (+20% fuel) x turbine T6 (redline
15,000 -> `rpm/500` = x30 instead of x24) x Build Tier 7 (313 pillars vs 201) takes the maxed plant
from ~1.10B to roughly `1.10B x 1.15 x 1.20 x (30/24) x (313/201) = ~2.95B EU/t` of *demand*. That is
a meaningful earned climb, and it cannot detonate anything: `outputCeiling()` already clamps at
`Integer.MAX_VALUE` (`:4065`) and `onRunningTick` clamps the injection to `outputLimit()` (`:3964`),
which is the only condition `addEnergyOutputMultipleDynamos` explodes over.

### 5.4 Where the multipliers attach — exactly

**One shared arithmetic**, per the class's own rule at `:3993-3995`. There are two derivation sites
and they must not diverge:

```java
/** The ONLY place progression multipliers enter the EU chain. */
public long euDemand() {
    return euDemandOf(spineBoostedSteam(true), rpmMilli, avgRotorFactorMilli, progOutputMilli());
}

/** New 4-arg overload. The existing 3-arg one delegates with progMilli = 1000, so every existing
 *  caller and every existing readout keeps its exact current answer. */
public static long euDemandOf(long afterSpine, int rpmMilliValue, int rotorMilli, int progMilli) {
    return afterSpine * rpmMilliValue / 500_000L * rotorMilli / 1000L * progMilli / 1000L;
}
```

- `:3953` becomes `final long demand = euDemand();`
- `:5998` becomes `return euDemand();`
- `progSteamMilli` and `progFuelMilli` apply **at `:3815`**, inside the `steamBase` sum, so they flow
  into `fuelBaseSteam` and therefore into the panel's step-1 line for free.
- **The math panel must be told.** `mathTurbineLine` (`:6640-6672`) prints the derivation as text. If
  `progOutputMilli() != 1000` and the line does not print it, the panel lies about its own
  arithmetic — the one thing this codebase's review record cares about most. Add a `progMilli`
  parameter, print a `" x prog x1.15"` clause, and sync the value.

---

## 6. PERSISTENCE PLAN — the highest-risk part

### 6.1 What the Godforge does (the pattern to copy — with one deliberate deviation)

`MTEForgeOfGods.java:1026-1042`:

```java
@Override public void setItemNBT(NBTTagCompound NBT)  { data.serializeNBT(NBT, false);
                                                        super.saveNBTData(NBT); }
@Override public void saveNBTData(NBTTagCompound NBT) { data.serializeNBT(NBT, true);
                                                        data.serializeRenderNBT(NBT);
                                                        super.saveNBTData(NBT); }
@Override public void loadNBTData(NBTTagCompound NBT) { data.deserializeNBT(NBT);
                                                        super.loadNBTData(NBT); }
```

The load-bearing idea: **one serializer object, called from both write paths, read by one read path.**
The two writers physically cannot drift because there is only one list of keys
(`ForgeOfGodsData.serializeNBT` at `:422`, `UpgradeStorage.serializeToNBT` at `:190`).

The `force` flag is the item-vs-tile discriminator: with `force = false` (the item path) only
non-default fields are written, so an unused controller drops as a clean tag-less item.

**The one thing NOT to copy:** the Godforge's `setItemNBT` calls `super.saveNBTData(NBT)`, dragging
the *entire* tile state onto the item. Do **not** do that here. This controller's `setItemNBT`
(`:5726`) deliberately calls `super.setItemNBT(aNBT)` and carries only four module counters, because
M2 established that carrying tile state across a wrench is exactly how `btsLastBuiltTier` armed a
destructive path. Keep that. Progress is additive property, so it joins the four counters;
`btsReactorTier`, `btsColumnRods`, `btsSteamPerTick` and the rest stay off the item.

### 6.2 Why the item path works at all (verified, not assumed)

- Break/wrench: `BaseMetaTileEntity.getDrops()` (`:1404-1418`) builds a fresh `NBTTagCompound`, calls
  `mMetaTileEntity.setItemNBT(tNBT)`, then `if (!tNBT.hasNoTags()) rStack.setTagCompound(tNBT);`
- Re-place: `ItemMachines.placeBlock` -> `tTileEntity.setInitialValuesAsNBT(aStack.getTagCompound(), tDamage)`
  (**server side only** — the client is passed `null`) -> `BaseMetaTileEntity.setInitialValuesAsNBT`
  (`:166-206`) -> `loadMetaTileNBT(aNBT)` -> `CommonBaseMetaTileEntity.loadMetaTileNBT:328` ->
  `getMetaTileEntity().loadNBTData(aNBT)`.

So **item NBT lands in the same `loadNBTData` as tile NBT.** Same keys, same reader, no second code
path — which is why the "one serializer, two writers" shape is safe.

### 6.3 The exact keys

All under the existing `bts` prefix. Written by **one** method, called from **both** writers.

```java
/** The ONE progression serializer. saveNBTData and setItemNBT both call this, so the reload path
 *  and the wrench path physically cannot carry different keys. */
private void writeProgress(NBTTagCompound aNBT) {
    aNBT.setInteger("btsProgVersion", PROG_NBT_VERSION);   // = 1
    aNBT.setLong   ("btsProgUpTicks", progUpTicks);
    aNBT.setLong   ("btsProgBurnup",  progBurnup);
    aNBT.setLong   ("btsProgReproc",  progReproc);
    aNBT.setLong   ("btsProgPerfect", progPerfect);
    aNBT.setInteger("btsProgSpent",   progPointsSpent);    // diagnostics only; recomputed on load
    final NBTTagCompound nodes = new NBTTagCompound();
    for (final BTSUpgrade u : BTSUpgrade.VALUES) {
        if (progNodes.contains(u)) nodes.setBoolean(u.name(), true);   // NAME, not ordinal
    }
    aNBT.setTag("btsProgNodes", nodes);
}

private void readProgress(NBTTagCompound aNBT) {
    progUpTicks = Math.max(0, aNBT.getLong("btsProgUpTicks"));
    progBurnup  = Math.max(0, aNBT.getLong("btsProgBurnup"));
    progReproc  = Math.max(0, aNBT.getLong("btsProgReproc"));
    progPerfect = Math.max(0, aNBT.getLong("btsProgPerfect"));
    progNodes.clear();
    final NBTTagCompound nodes = aNBT.getCompoundTag("btsProgNodes");
    for (final BTSUpgrade u : BTSUpgrade.VALUES) if (nodes.getBoolean(u.name())) progNodes.add(u);
    pruneInvalidNodes();                  // drop any node whose prereqs are not satisfied
    progPointsSpent = recomputeSpent();   // derive from the surviving node set; never trust NBT
    // tiers are DERIVED from the counters on every read - nothing to persist, nothing to desync
}
```

| Key | Type | Meaning |
|---|---|---|
| `btsProgVersion` | int | schema version, for future migration |
| `btsProgUpTicks` | long | generating ticks |
| `btsProgBurnup` | long | `ROD_MULT`-weighted segments burned |
| `btsProgReproc` | long | Derrick + Annex loads deposited |
| `btsProgPerfect` | long | 8/8-timed turbine cycles that had steam |
| `btsProgNodes` | compound of booleans keyed by **enum name** | purchased nodes |
| `btsProgSpent` | int | written for diagnostics; **recomputed on load, not trusted** |

Three deliberate choices, each a departure from the Godforge:

1. **Node keys are `u.name()`, not `u.ordinal()`.** The Godforge uses ordinals
   (`UpgradeStorage.java:196`, `:216`), which silently reassigns every unlock the moment a node is
   inserted mid-enum. Names cost nothing and survive reordering; a *renamed* node loses only itself,
   and `pruneInvalidNodes()` keeps the graph consistent.
2. **Tiers are never persisted.** `tier = count(thresholds <= counter)` is pure. Persisting it
   creates a second source of truth that can disagree with the counter — the drift class this
   codebase already fights in its formatters.
3. **`progPointsSpent` is recomputed from the node set on load.** If NBT and the node set ever
   disagree, the node set is the physical fact.

`onPostTick` recomputes `progPointsAvailable = earnedFromTiers() - progPointsSpent` on a 1 s cadence
(the Godforge uses 5 s at `MTEForgeOfGods.java:431`).

### 6.4 Where the calls go

```java
@Override public void saveNBTData(NBTTagCompound aNBT) {
    super.saveNBTData(aNBT);
    ... existing ~30 keys ...
    writeProgress(aNBT);                 // append, end of :5648
}

@Override public void setItemNBT(NBTTagCompound aNBT) {
    super.setItemNBT(aNBT);              // KEEP - do NOT switch to super.saveNBTData
    ... existing four module counters ...
    writeProgress(aNBT);                 // append, end of :5726
}

@Override public void loadNBTData(NBTTagCompound aNBT) {
    super.loadNBTData(aNBT);
    ... existing reads ...
    readProgress(aNBT);                  // append, end of :5735
}
```

And **`resetPieceState()` (`:2713`) must not touch any `prog*` field.** That method is already the
documented home of this invariant for `derrickProgress` / `annexProgress` (`:5340-5344`, `:5386-5393`):
a structure check fires on every neighbour update, so anything zeroed there dies the moment a player
breaks one casing to route a pipe. Add the progression fields to that method's comment explicitly,
so the next reviewer can see the exclusion is deliberate rather than forgotten.

### 6.5 The four survival cases, and how to verify each

| Case | Mechanism | Test |
|---|---|---|
| **World reload** | `saveNBTData` -> `loadNBTData` | `/btsprogress set burnup 50000`, quit to menu, reload, reopen panel: burnup 50,000, tier 4 |
| **Chunk unload/reload** | same pair via `writeToNBT`/`readFromNBT` | fly 300+ blocks away and back, reopen |
| **WRENCH** | `getDrops()` -> `setItemNBT` -> item tag -> `placeBlock` -> `setInitialValuesAsNBT` -> `loadNBTData` | wrench the controller, **inspect the dropped item's NBT for `btsProgNodes`**, re-place, reopen |
| **Un-form / re-form** | `resetPieceState()` excludes `prog*`; `checkMachine` never writes them | break one Solid Steel casing (plant un-forms), replace it, reopen |

The wrench case is the one that historically fails silently, so **verify it on the item, not only on
the replaced block**. If `setItemNBT` wrote nothing, `getDrops()`'s `if (!tNBT.hasNoTags())` leaves
the stack tag-less and the loss is invisible until re-placement.

Cheap automated guard worth adding: `/btsprogress verify` serialises to a scratch `NBTTagCompound`
via `writeProgress`, zeroes the live fields, reads them back with `readProgress`, and reports any
mismatch. ~20 lines, and it catches a forgotten key the moment it is introduced.

### 6.6 The debug command (the brief's "no back-fill, but ship a setter")

`CommandBTSProgress`, registered in `CommonProxy.serverStarting` next to the existing five
(`CommonProxy.java:35-40`), permission level 2, resolving the controller with
`BTSCommands.nearestStation(player)` exactly like `CommandBTSClock`:

```
/btsprogress                                    print all four counters, tiers, points, nodes
/btsprogress set <uptime|burnup|reproc|turbine> <n>
/btsprogress tier <line> <0-7>                  set that counter to the tier's threshold
/btsprogress points                             print earned / spent / available
/btsprogress unlockall | resetall               the Godforge's two debug buttons, as a command
/btsprogress verify                             the NBT round-trip self-check from 6.5
```

`unlockall` / `resetall` mirror `UpgradeStorage.unlockAll()` / `resetAll()` and the two debug buttons
in `UpgradeTreePanel.getDebugWidgets` (`:271-320`).

---

## 7. The four axes, checked against this plan

1. **CLIENT vs SERVER.** Every `prog*` field is server-written (`onRunningTick`, `checkProcessing`,
   `evaluateCycle`, `runDerrickTick`, `runAnnexTick`, `loadNBTData`). Every panel value must come
   from a sync handler. `construct(..., hintsOnly=true)` must not consult progression at all — which
   is why Build Tier 6/7 is gated in `setBuildTierSetting`, never in the shape registry.
2. **TILE NBT vs ITEM NBT.** Section 6. One `writeProgress` called from both writers; keep
   `super.setItemNBT`; do **not** adopt the Godforge's `super.saveNBTData` in the item path.
3. **PLANT-WIDE HATCH LISTS.** Nothing in this plan registers a hatch or reads
   `mMaintenanceHatches` / `mEnergyHatches`. The one adjacent risk is the **3rd Cooling Tower**
   unlock (uptime T5): `coolingTowerCount()` (`:612`) is `towerFrontBuilt + towerBackBuilt`, two
   named booleans rather than a list, so a third tower needs a third flag and a third structure
   piece — not a list entry. Say so in that ticket.
4. **`IStructureElementChain`.** Nothing here adds a fallback chain. The Build Tier 6/7 shapes are
   new whole pieces registered unconditionally, so `spawnHint` / `placeBlock` behaviour is unchanged.

---

## 8. Build order for the implementing units

1. **`BTSProgression` + the four counters + `writeProgress`/`readProgress` + `CommandBTSProgress`.**
   Ship and verify all four survival cases *before anything else exists*. This is the part that
   loses the campaign if it is wrong.
2. `BTSMilestone` enum + threshold tables + tier derivation + the two milestone panels. **Read-only —
   grants nothing yet.** Verify the numbers against a live plant.
3. The multipliers (tiers 1-4) via `progOutputMilli` / `progSteamMilli` / `progFuelMilli` /
   `progRpmGainMilli` / `progRotorMilli`, plus the `mathTurbineLine` text change. All default to
   1000, so step 3 must be a *bit-identical no-op* on a plant with no progress — that is its
   regression test.
4. `BTSUpgrade` enum + `BTSUpgradeTreePanel` + detail panel + buy/refund, with server-side
   re-validation in the C2S handler mirroring `ForgeOfGodsData.unlockUpgrade`.
5. The unlocks (tiers 5-7). Each is a *new maximum*; each gets its own commit and its own "the plant
   that stands today is unchanged" check.

### 8.1 The tree shape (25 nodes, cost sum = 112 = a full milestone sweep)

| Row | Nodes | Cost each | Row total | Running |
|---|---|---|---|---|
| root | `START` | 0 | 0 | 0 |
| 1 | 3 | 1 | 3 | 3 |
| 2 | 4 | 2 | 8 | 11 |
| 3 | 4 | 3 | 12 | 23 |
| 4 | 3 | 4 | 12 | 35 |
| 5 | 3 | 5 | 15 | 50 |
| 6 | 2 | 6 | 12 | 62 |
| 7 | 2 | 8 | 16 | 78 |
| 8 | 1 | 10 | 10 | 88 |
| 9 | 1 | 12 | 12 | 100 |
| omega | 1 | 12 | 12 | **112** |

Same property as the Godforge: a full milestone sweep buys the whole tree exactly, nothing left over
and nothing short. Rows 1-4 (35 points, reachable at roughly all-tier-4s, i.e. inside the first day
and a half) hold the everyday efficiency nodes; rows 5+ hold the branching choices.

Geometry: a **292 x ~960** canvas with `BUTTON_W = 40`, `BUTTON_H = 15`, ~60px row spacing — the
Godforge's exact numbers, which are known to fit and known to scroll.

**Improvement over the Godforge to make here:** derive the connector-line list from the prereq edges
(`for each node, for each prereq -> createConnectorLine(node.colour(), prereq, node)`) instead of
hand-maintaining a second 37-entry list that can silently disagree with the graph.
