KRunes v2.0

Dynamic Rune System for Minecraft with customizable particles, sounds, and commands.

📌 Table of Contents

Plugin Information

Plugin Items

Effects Configuration

Messages

Class Structure

Usage Flow

Placeholders

Flow Diagrams

📝 Plugin Information
name: KRunes
version: 2.0
main: me.erik.kRunes.KRunes
api-version: 1.21

commands:
  Krune:
    description: Gives the Rune Chalk.
    usage: /KRune


/Krune command: gives the Rune Chalk to the player.

API version: 1.21

Main class: me.erik.kRunes.KRunes

🪄 Plugin Items
Item	Name	Material	CustomModelData	Description
creation	&dCreation Staff	STICK	1001	Use to draw the rune: %rune%
Maximum 9 blocks
Command: %command%
chalk	&bRune Chalk	STICK	1002	Use to draw runes on the ground
activator	&bRune Activator	BLAZE_ROD	1003	Use to activate runes on the ground
✨ Effects Configuration (config.yml)
Particles
Type	Particle	Amount	Speed (force)
draw	REVERSE_PORTAL	5	0.01
activate	TOTEM_OF_UNDYING	100	0.5
fail	SMOKE_NORMAL	100	1.0
Sounds
Type	Sound	Pitch	Volume
draw	BLOCK_AMETHYST_BLOCK_STEP	1.0	1.0
activate	ENTITY_PLAYER_LEVELUP	1.2	1.0
fail	ENTITY_GENERIC_EXPLODE	1.5	3.0
💬 Messages (messages.yml)
General / Prefix
general:
  prefix: "&7[&bKRunes&7] "
  player_only: "&cOnly players can use this command!"
  no_permission: "&cYou don't have permission to do this!"
  reload_success: "&aReloaded!"
  unknown_command: "&cUnknown command. Use /krunes help"

Errors
errors:
  invalid_rune: "&cThis rune doesn't exist!"
  not_active: "&cThis rune is incomplete!"
  missing_argument: "&cInsufficient arguments. Use &e/krunes help"

Rune
rune:
  start: "&aRune has started!"
  success: "&aYou activated the Rune!"
  fail: "&cThe rune failed!"
  complete: "&aThe rune has been completed!"

Commands
command:
  give_fail: "&aThis item does not exist!"

🏗 Class Structure
Class	Responsibility
KRunes	Initializes the plugin, registers commands and events
DataManager	Handles persistence of runes and temporary creations
MessageManager	Loads and provides configurable messages
EffectsManager	Handles particles, sounds, and persistent lines
RuneManager	Handles rune creation, drawing, and activation
Internal	RuneData, PlayerCreationData, PlayerDrawingData, ParticleData, SoundData
🔄 Usage Flow

Player runs /Krune → receives Rune Chalk

Uses Creation Staff → starts a new rune (startRuneCreation)

Draws blocks → lines and particles appear (addCreationBlock)

Completes rune → saved to disk (saveRuneFromCreation)

Activates rune with Rune Activator → commands execute, particles and sounds triggered (tryActivateRune)

📝 Placeholders
Placeholder	Description
%rune%	Name of the rune being drawn or created
%command%	Command associated with the rune
%player%	Name of the player executing the action
📊 Flow Diagrams
Rune Creation
flowchart TD
    A[Player starts rune] --> B[Creation: PlayerCreationData]
    B --> C[Draw blocks with Staff]
    C --> D{Blocks >= required?}
    D -->|Yes| E[Rune saved]
    D -->|No| C

Rune Activation
flowchart TD
    A[Player uses Activator] --> B[Check current drawing]
    B --> C{Matches any rune?}
    C -->|Yes| D[Activate rune: commands, particles, sounds]
    C -->|No| E[Error: rune incomplete]
