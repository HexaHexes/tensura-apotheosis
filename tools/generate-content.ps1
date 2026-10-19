# Generates all tensura_apotheosis datapack content: affixes, gems, lang, gem models.
# Single source of truth: the affix/gem tables below also emit the lang file.
$ErrorActionPreference = 'Stop'

$root = 'C:\Games\Minecraft\Mods\tensura-apotheosis\src\main\resources'
$data = Join-Path $root 'data\tensura_apotheosis'
$assets = Join-Path $root 'assets\tensura_apotheosis'
$apoAssets = Join-Path $root 'assets\apotheosis'

$utf8 = New-Object System.Text.UTF8Encoding($false)

function WriteJsonFile([string]$path, $obj) {
    $dir = Split-Path $path -Parent
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    [System.IO.File]::WriteAllText($path, ($obj | ConvertTo-Json -Depth 12), $utf8)
}

function Range($min, $max, $step = $null) {
    if ($null -eq $step) {
        [pscustomobject][ordered]@{ min = [double]$min; max = [double]$max }
    } else {
        [pscustomobject][ordered]@{ min = [double]$min; max = [double]$max; step = [double]$step }
    }
}

# --- category tables ----------------------------------------------------------
$MEA  = @('apotheosis:melee_weapon', 'apotheosis:trident')
$BOW  = @('apotheosis:bow')
$BODY = @('apotheosis:chestplate', 'apotheosis:leggings')
$LB   = @('apotheosis:leggings', 'apotheosis:boots')
$ARMO = @('apotheosis:helmet', 'apotheosis:chestplate', 'apotheosis:leggings', 'apotheosis:boots')
$SHLD = @('apotheosis:shield')
$BRK  = @('apotheosis:breaker')
$ALL  = @('apotheosis:bow', 'apotheosis:breaker', 'apotheosis:helmet', 'apotheosis:chestplate',
          'apotheosis:leggings', 'apotheosis:boots', 'apotheosis:shield', 'apotheosis:trident',
          'apotheosis:melee_weapon', 'apotheosis:shears')

$R = 'apotheosis:rare'; $E = 'apotheosis:epic'; $M = 'apotheosis:mythic'
$U = 'apotheosis:uncommon'; $C = 'apotheosis:common'

$affixes = @()   # @{ rel; name; suffix; json }
$lang = [ordered]@{}

function Add-Affix([string]$rel, [string]$name, [string]$suffix, $json) {
    $script:affixes += @{ rel = $rel; name = $name; suffix = $suffix; json = $json }
}

function New-AttrAffix([string]$attr, $cats, [string]$op, $values, [int]$weight = 25, $exclusive = @()) {
    [pscustomobject][ordered]@{
        type       = 'apotheosis:attribute'
        attribute  = $attr
        categories = $cats
        definition = [pscustomobject][ordered]@{
            affix_type    = 'stat'
            exclusive_set = $exclusive
            weights       = [pscustomobject]@{ quality = 0.1; weight = $weight }
        }
        operation  = $op
        values     = $values
    }
}

function New-ProcAffix([string]$effect, [string]$target, $types, $values, [int]$weight = 25, $exclusive = @(), [bool]$stack = $true) {
    $d = [ordered]@{
        type         = 'apotheosis:mob_effect'
        definition   = [pscustomobject][ordered]@{
            affix_type    = 'basic_effect'
            exclusive_set = $exclusive
            weights       = [pscustomobject]@{ quality = 0.1; weight = $weight }
        }
        mob_effect   = $effect
        target       = $target
        types        = $types
        values       = $values
    }
    if ($stack) {
        # stack fields go directly after mob_effect, matching Apotheosis' own layout
        $final = [ordered]@{}
        foreach ($k in $d.Keys) {
            $final[$k] = $d[$k]
            if ($k -eq 'mob_effect') {
                $final['stack_on_reapply'] = $true
                $final['stacking_limit'] = 3
            }
        }
        return [pscustomobject]$final
    }
    [pscustomobject]$d
}

# ======================================================================
# MELEE - attribute affixes (scales mirror Apotheosis' own affixes)
# ======================================================================
Add-Affix 'melee/attribute/flame_blade' 'Flame Blade' 'of the Salamander' (
    New-AttrAffix 'apothic_attributes:fire_damage' $MEA 'add_value' ([ordered]@{
        $U = Range 2 4; $R = Range 2 5; $E = Range 4 6; $M = Range 4 10 }))
Add-Affix 'melee/attribute/frost_blade' 'Frost Blade' 'of the Yeti' (
    New-AttrAffix 'apothic_attributes:cold_damage' $MEA 'add_value' ([ordered]@{
        $U = Range 2 4; $R = Range 2 5; $E = Range 4 6; $M = Range 4 10 }))
Add-Affix 'melee/attribute/armor_piercing' 'Armor Piercing' 'of the Breaker' (
    New-AttrAffix 'apothic_attributes:armor_pierce' $MEA 'add_value' ([ordered]@{
        $R = Range 4 8; $E = Range 5 10; $M = Range 5 12 }))
Add-Affix 'melee/attribute/lethal' 'Lethal' 'of the Assassin' (
    New-AttrAffix 'apothic_attributes:crit_chance' $MEA 'add_value' ([ordered]@{
        $R = Range 0.1 0.25; $E = Range 0.15 0.35; $M = Range 0.25 0.55 }))
Add-Affix 'melee/attribute/keen' 'Keen' 'of the Edge' (
    New-AttrAffix 'apothic_attributes:crit_damage' $MEA 'add_value' ([ordered]@{
        $R = Range 0.15 0.25; $E = Range 0.15 0.3; $M = Range 0.25 0.4 }))
Add-Affix 'melee/attribute/soul_thief' 'Soul Thief' 'of Predation' (
    New-AttrAffix 'apothic_attributes:life_steal' $MEA 'add_value' ([ordered]@{
        $R = Range 0.05 0.1; $E = Range 0.08 0.13; $M = Range 0.1 0.15 }))
Add-Affix 'melee/attribute/predators_edge' "Predator's Edge" 'of the Hunt' (
    New-AttrAffix 'apothic_attributes:current_hp_damage' $MEA 'add_value' ([ordered]@{
        $E = Range 0.1 0.2; $M = Range 0.1 0.25 }))

# ======================================================================
# MELEE - proc (mob_effect on attack_target); DoT family: uncommon+
# ======================================================================
$dotVals = [ordered]@{
    $U = [pscustomobject]@{ duration = (Range 80 100 20); amplifier = 0; cooldown = 80 }
    $R = [pscustomobject]@{ duration = (Range 100 120 20); amplifier = 0; cooldown = 80 }
    $E = [pscustomobject]@{ duration = (Range 120 160 20); amplifier = (Range 0 1 0.5); cooldown = 70 }
    $M = [pscustomobject]@{ duration = (Range 140 200 20); amplifier = (Range 0 1 0.5); cooldown = 60 }
}
Add-Affix 'melee/mob_effect/frostbite' 'Frostbite' 'of the Frost Queen' (
    New-ProcAffix 'tensura:frost' 'attack_target' $MEA $dotVals)
Add-Affix 'melee/mob_effect/infective' 'Infective' 'of the Contagion' (
    New-ProcAffix 'tensura:infection' 'attack_target' $MEA $dotVals)
Add-Affix 'melee/mob_effect/venomous' 'Venomous' 'of the Poison Breath' (
    New-ProcAffix 'tensura:magicule_poison' 'attack_target' $MEA $dotVals)
Add-Affix 'melee/mob_effect/corrosive' 'Corrosive' 'of Dissolution' (
    New-ProcAffix 'tensura:corrosion' 'attack_target' $MEA $dotVals)

# Hard-CC family: rare+, lower weight, mutually exclusive.
$ccVals = [ordered]@{
    $R = [pscustomobject]@{ duration = (Range 60 80 20); amplifier = 0; cooldown = 120 }
    $E = [pscustomobject]@{ duration = (Range 80 100 20); amplifier = 0; cooldown = 110 }
    $M = [pscustomobject]@{ duration = (Range 100 120 20); amplifier = (Range 0 1 0.5); cooldown = 100 }
}
$ccSet = @(
    'tensura_apotheosis:melee/mob_effect/paralyzing',
    'tensura_apotheosis:melee/mob_effect/silencing',
    'tensura_apotheosis:melee/mob_effect/terrifying',
    'tensura_apotheosis:melee/mob_effect/stone_gaze')
function CcExcl([string]$self) { @($ccSet | Where-Object { $_ -ne $self }) }

Add-Affix 'melee/mob_effect/paralyzing' 'Paralyzing' 'of the Bound' (
    New-ProcAffix 'tensura:paralysis' 'attack_target' $MEA $ccVals 20 (CcExcl 'tensura_apotheosis:melee/mob_effect/paralyzing'))
Add-Affix 'melee/mob_effect/silencing' 'Silencing' 'of Severance' (
    New-ProcAffix 'tensura:silence' 'attack_target' $MEA $ccVals 20 (CcExcl 'tensura_apotheosis:melee/mob_effect/silencing'))
Add-Affix 'melee/mob_effect/terrifying' 'Terrifying' 'of Dread' (
    New-ProcAffix 'tensura:fear' 'attack_target' $MEA $ccVals 20 (CcExcl 'tensura_apotheosis:melee/mob_effect/terrifying'))
$gazeVals = [ordered]@{
    $E = [pscustomobject]@{ duration = (Range 40 60 20); amplifier = 0; cooldown = 160 }
    $M = [pscustomobject]@{ duration = (Range 60 80 20); amplifier = 0; cooldown = 140 }
}
Add-Affix 'melee/mob_effect/stone_gaze' 'Stone Gaze' 'of Petrification' (
    New-ProcAffix 'tensura:petrification' 'attack_target' $MEA $gazeVals 10 (CcExcl 'tensura_apotheosis:melee/mob_effect/stone_gaze'))

# ======================================================================
# RANGED
# ======================================================================
Add-Affix 'ranged/attribute/hawk_eye' 'Hawk Eye' 'of the Sentinel' (
    New-AttrAffix 'apothic_attributes:arrow_damage' $BOW 'add_multiplied_total' ([ordered]@{
        $R = Range 0.2 0.3; $E = Range 0.25 0.35; $M = Range 0.25 0.4 }))
Add-Affix 'ranged/attribute/quick_draw' 'Quick Draw' 'of the Swift' (
    New-AttrAffix 'apothic_attributes:draw_speed' $BOW 'add_multiplied_total' ([ordered]@{
        $R = Range 0.3 0.5; $E = Range 0.4 0.6; $M = Range 0.5 0.65 }))
Add-Affix 'ranged/attribute/warped' 'Warped' 'of Spatial Rending' (
    New-AttrAffix 'tensura:warp_shot' $BOW 'add_value' ([ordered]@{
        $R = Range 1 2; $E = Range 2 3; $M = Range 3 5 }))

$rangedDot = [ordered]@{
    $R = [pscustomobject]@{ duration = (Range 100 120 20); amplifier = 0; cooldown = 80 }
    $E = [pscustomobject]@{ duration = (Range 120 160 20); amplifier = (Range 0 1 0.5); cooldown = 70 }
    $M = [pscustomobject]@{ duration = (Range 140 200 20); amplifier = (Range 0 1 0.5); cooldown = 60 }
}
Add-Affix 'ranged/mob_effect/frostbite' 'Frostbite' 'of the Frost Queen' (
    New-ProcAffix 'tensura:frost' 'arrow_target' $BOW $rangedDot)
Add-Affix 'ranged/mob_effect/infective' 'Infective' 'of the Contagion' (
    New-ProcAffix 'tensura:infection' 'arrow_target' $BOW $rangedDot)

# ======================================================================
# ARMOR - attribute affixes
# ======================================================================
Add-Affix 'armor/attribute/magicule_fortified' 'Magicule Fortified' 'of the Magicule' (
    New-AttrAffix 'tensura:max_magicule' $BODY 'add_value' ([ordered]@{
        $R = Range 30 60; $E = Range 50 100; $M = Range 80 160 }))
Add-Affix 'armor/attribute/magicule_attuned' 'Magicule Attuned' 'of Accumulation' (
    New-AttrAffix 'tensura:magicule_gain' $BODY 'add_value' ([ordered]@{
        $R = Range 0.05 0.25; $E = Range 0.1 0.35; $M = Range 0.15 0.5 }))
Add-Affix 'armor/attribute/aura_attuned' 'Aura Attuned' 'of the Aura' (
    New-AttrAffix 'tensura:max_aura' $BODY 'add_value' ([ordered]@{
        $R = Range 20 40; $E = Range 35 70; $M = Range 60 120 }))
Add-Affix 'armor/attribute/aura_blessed' 'Aura Blessed' 'of the Spirit' (
    New-AttrAffix 'tensura:aura_gain' $BODY 'add_value' ([ordered]@{
        $R = Range 0.05 0.25; $E = Range 0.1 0.35; $M = Range 0.15 0.5 }))
Add-Affix 'armor/attribute/spiritually_fortified' 'Spiritually Fortified' 'of the Soul' (
    New-AttrAffix 'tensura:max_spiritual_health' $BODY 'add_value' ([ordered]@{
        $R = Range 10 25; $E = Range 15 40; $M = Range 25 60 }))
Add-Affix 'armor/attribute/mending_soul' 'Mending Soul' 'of Rejuvenation' (
    New-AttrAffix 'tensura:spiritual_health_regeneration' $BODY 'add_value' ([ordered]@{
        $R = Range 0.5 1; $E = Range 0.75 1.5; $M = Range 1 2 }))
Add-Affix 'armor/attribute/arcane_barrier' 'Arcane Barrier' 'of Warding' (
    New-AttrAffix 'tensura:magic_barrier' $BODY 'add_value' ([ordered]@{
        $R = Range 3 6; $E = Range 4 8; $M = Range 6 12 }))
Add-Affix 'armor/attribute/iron_will' 'Iron Will' 'of the Bulwark' (
    New-AttrAffix 'tensura:physical_barrier' $BODY 'add_value' ([ordered]@{
        $R = Range 3 6; $E = Range 4 8; $M = Range 6 12 }))
Add-Affix 'armor/attribute/warded' 'Warded' 'of Null Magic' (
    New-AttrAffix 'tensura:magic_resistance' $ARMO 'add_value' ([ordered]@{
        $R = Range 3 6; $E = Range 4 9; $M = Range 6 12 }))
Add-Affix 'armor/attribute/sanctified' 'Sanctified' 'of the Holy' (
    New-AttrAffix 'tensura:light_resistance' $BODY 'add_value' ([ordered]@{
        $R = Range 3 6; $E = Range 4 9; $M = Range 6 12 }))
Add-Affix 'armor/attribute/indomitable' 'Indomitable' 'of Glory' (
    New-AttrAffix 'apothic_attributes:overheal' $BODY 'add_value' ([ordered]@{
        $R = Range 0.1 0.2; $E = Range 0.1 0.25; $M = Range 0.15 0.25 }))
Add-Affix 'armor/attribute/regenerative' 'Regenerative' 'of Vigor' (
    New-AttrAffix 'apothic_attributes:healing_received' $BODY 'add_multiplied_base' ([ordered]@{
        $R = Range 0.1 0.25; $E = Range 0.15 0.3; $M = Range 0.2 0.4 }))
Add-Affix 'armor/attribute/untouchable' 'Untouchable' 'of Evasion' (
    New-AttrAffix 'tensura:melee_dodge_chance' $LB 'add_value' ([ordered]@{
        $R = Range 2 4; $E = Range 3 6; $M = Range 5 9 }))
Add-Affix 'armor/attribute/ghost_step' 'Ghost Step' 'of Phantoms' (
    New-AttrAffix 'tensura:projectile_dodge_chance' $LB 'add_value' ([ordered]@{
        $R = Range 2 4; $E = Range 3 6; $M = Range 5 9 }))
Add-Affix 'armor/attribute/concealed' 'Concealed' 'of the Shadow' (
    New-AttrAffix 'tensura:presence_concealment' $ARMO 'add_value' ([ordered]@{
        $R = Range 5 12; $E = Range 8 18; $M = Range 12 25 }))

# ARMOR - hurt_self proc (magicule regeneration when struck)
$surgeVals = [ordered]@{
    $R = [pscustomobject]@{ duration = 100; amplifier = 0; cooldown = 200 }
    $E = [pscustomobject]@{ duration = 140; amplifier = 0; cooldown = 180 }
    $M = [pscustomobject]@{ duration = 180; amplifier = (Range 0 1 0.5); cooldown = 160 }
}
Add-Affix 'armor/mob_effect/magicule_surge' 'Magicule Surge' 'of Recovery' (
    New-ProcAffix 'tensura:magicule_regeneration' 'hurt_self' $BODY $surgeVals 25 @() $false)

# ======================================================================
# SHIELD
# ======================================================================
Add-Affix 'shield/attribute/bulwark' 'Bulwark' 'of the Guardian' (
    New-AttrAffix 'tensura:magic_barrier' $SHLD 'add_value' ([ordered]@{
        $R = Range 3 6; $E = Range 4 8; $M = Range 6 12 }))
Add-Affix 'shield/attribute/aegis' 'Aegis' 'of Protection' (
    New-AttrAffix 'tensura:physical_barrier' $SHLD 'add_value' ([ordered]@{
        $R = Range 3 6; $E = Range 4 8; $M = Range 6 12 }))
Add-Affix 'shield/attribute/wardstone' 'Wardstone' 'of Nullification' (
    New-AttrAffix 'tensura:magic_resistance' $SHLD 'add_value' ([ordered]@{
        $R = Range 3 6; $E = Range 4 9; $M = Range 6 12 }))

# ======================================================================
# BREAKER (tools)
# ======================================================================
Add-Affix 'breaker/attribute/deep_delver' 'Deep Delver' 'of the Depths' (
    New-AttrAffix 'apothic_attributes:mining_speed' $BRK 'add_multiplied_total' ([ordered]@{
        $R = Range 0.25 0.5; $E = Range 0.25 0.7; $M = Range 0.55 0.85 }))
Add-Affix 'breaker/attribute/learned' 'Learned' 'of Wisdom' (
    New-AttrAffix 'apothic_attributes:experience_gained' $BRK 'add_multiplied_total' ([ordered]@{
        $R = Range 0.35 0.5; $E = Range 0.35 0.6; $M = Range 0.55 0.65 }))
Add-Affix 'breaker/attribute/earth_affinity' 'Earth Affinity' 'of the Earth Dragon' (
    New-AttrAffix 'tensura:earth_boost' $BRK 'add_value' ([ordered]@{
        $R = Range 2 5; $E = Range 3 7; $M = Range 5 10 }))

# ======================================================================
# GENERIC (all equipment)
# ======================================================================
Add-Affix 'generic/attribute/swift_chant' 'Swift Chant' 'of Quickening' (
    New-AttrAffix 'tensura:chant_speed' $ALL 'add_multiplied_total' ([ordered]@{
        $R = Range 0.05 0.1; $E = Range 0.1 0.15; $M = Range 0.15 0.25 }))
Add-Affix 'generic/attribute/efficient_casting' 'Efficient Casting' 'of Conservation' (
    New-AttrAffix 'tensura:magic_cost_multiplier' $ALL 'add_value' ([ordered]@{
        $R = Range (-0.1) (-0.05); $E = Range (-0.15) (-0.08); $M = Range (-0.2) (-0.1) }))
Add-Affix 'generic/attribute/prodigy' 'Prodigy' 'of Talents' (
    New-AttrAffix 'tensura:ability_learning_gain' $ALL 'add_value' ([ordered]@{
        $R = Range 0.1 0.3; $E = Range 0.2 0.5; $M = Range 0.3 0.8 }))
Add-Affix 'generic/attribute/mastered' 'Mastered' 'of the Savant' (
    New-AttrAffix 'tensura:ability_mastery_gain' $ALL 'add_value' ([ordered]@{
        $R = Range 0.1 0.3; $E = Range 0.2 0.5; $M = Range 0.3 0.8 }))
# NOTE: min/max ranges become StepFunctions with an implicit step of 0.01 -
# (max - min) must be a multiple of that step or the affix fails to parse.
Add-Affix 'generic/attribute/quickened' 'Quickened' 'of Haste' (
    New-AttrAffix 'apothic_attributes:cooldown_reduction' $ALL 'add_value' ([ordered]@{
        $R = Range 0.01 0.05; $E = Range 0.05 0.08; $M = Range 0.05 0.1 }))
Add-Affix 'generic/attribute/danger_sense' 'Danger Sense' 'of Awareness' (
    New-AttrAffix 'tensura:presence_sense_radius' $ALL 'add_value' ([ordered]@{
        $R = Range 3 8; $E = Range 5 12; $M = Range 8 16 }))
Add-Affix 'generic/attribute/analyst' 'Analyst' 'of Scrutiny' (
    New-AttrAffix 'tensura:analysis_level' $ALL 'add_value' ([ordered]@{
        $R = Range 1 2; $E = Range 2 3; $M = Range 3 5 }))
Add-Affix 'generic/attribute/far_sight' 'Far Sight' 'of Foresight' (
    New-AttrAffix 'tensura:analysis_distance' $ALL 'add_value' ([ordered]@{
        $R = Range 5 10; $E = Range 8 16; $M = Range 12 24 }))
Add-Affix 'generic/attribute/heatseeker' 'Heatseeker' 'of Detection' (
    New-AttrAffix 'tensura:heat_sense_radius' $ALL 'add_value' ([ordered]@{
        $R = Range 5 10; $E = Range 8 16; $M = Range 12 25 }))

# ======================================================================
# GEMS
# ======================================================================
$GC = @{
    body   = @{ key = 'tensura_body';   types = $BODY }
    helmet = @{ key = 'tensura_helmet'; types = @('apotheosis:helmet') }
    boots  = @{ key = 'tensura_boots';  types = @('apotheosis:boots') }
    weapon = @{ key = 'tensura_weapon'; types = $MEA }
    bow    = @{ key = 'tensura_bow';    types = $BOW }
    tool   = @{ key = 'tensura_tool';   types = @('apotheosis:breaker', 'apotheosis:shears') }
    shield = @{ key = 'tensura_shield'; types = $SHLD }
}

$flatW = [pscustomobject][ordered]@{ quality = 0.1; weight = 100 }
$uniW = [pscustomobject][ordered]@{
    frontier = [pscustomobject]@{ quality = 0.05; weight = 2 }
    ascent   = [pscustomobject]@{ quality = 0.08; weight = 6 }
    summit   = [pscustomobject]@{ quality = 0.1;  weight = 15 }
    pinnacle = [pscustomobject]@{ quality = 0.15; weight = 30 }
}

function Purity([double]$c, [double]$ch, [double]$fl, [double]$n, [double]$fla, [double]$p) {
    [pscustomobject][ordered]@{ cracked = $c; chipped = $ch; flawed = $fl; normal = $n; flawless = $fla; perfect = $p }
}

function GemLine($class, [string]$attr, [string]$op, $vals) {
    [pscustomobject][ordered]@{
        type      = 'apotheosis:attribute'
        attribute = $attr
        gem_class = [pscustomobject][ordered]@{ key = $GC[$class].key; types = $GC[$class].types }
        operation = $op
        values    = $vals
    }
}

$gems = @()   # @{ rel; name; json }
function Add-Gem([string]$family, [string]$name, [string]$display, $json) {
    $script:gems += @{ rel = "$family/$name"; name = $name; display = $display; json = $json }
}

function New-Gem($weights, $bonuses, [string]$minPurity = 'cracked', [bool]$unique = $false) {
    $o = [ordered]@{ type = 'apotheosis:gem'; weights = $weights }
    if ($minPurity -ne 'cracked') { $o['min_purity'] = $minPurity }
    if ($unique) { $o['unique'] = $true }
    $o['bonuses'] = $bonuses
    [pscustomobject]$o
}

Add-Gem 'core' 'magicule' 'Magicule Gem' (New-Gem $flatW @(
    (GemLine 'body' 'tensura:max_magicule' 'add_value' (Purity 20 35 55 90 140 210)),
    (GemLine 'weapon' 'tensura:magicule_gain' 'add_value' (Purity 0.05 0.08 0.12 0.18 0.25 0.35))))
Add-Gem 'core' 'aura' 'Aura Gem' (New-Gem $flatW @(
    (GemLine 'body' 'tensura:max_aura' 'add_value' (Purity 15 30 50 85 130 200)),
    (GemLine 'boots' 'tensura:aura_regeneration_multiplier' 'add_multiplied_total' (Purity 0.02 0.04 0.07 0.1 0.14 0.2))))
Add-Gem 'core' 'soul' 'Soul Gem' (New-Gem $flatW @(
    (GemLine 'body' 'tensura:max_spiritual_health' 'add_value' (Purity 8 14 22 35 55 80)),
    (GemLine 'helmet' 'tensura:spiritual_health_regeneration' 'add_value' (Purity 0.2 0.4 0.6 1 1.4 2))))
Add-Gem 'core' 'predator' 'Predator Gem' (New-Gem $flatW @(
    (GemLine 'weapon' 'apothic_attributes:life_steal' 'add_value' (Purity 0.02 0.03 0.05 0.07 0.09 0.12)),
    (GemLine 'bow' 'apothic_attributes:current_hp_damage' 'add_value' (Purity 0.03 0.05 0.08 0.12 0.16 0.22))))
Add-Gem 'core' 'doppelganger' 'Doppelganger Gem' (New-Gem $flatW @(
    (GemLine 'body' 'tensura:melee_dodge_chance' 'add_value' (Purity 1 2 3 5 7 10)),
    (GemLine 'boots' 'tensura:projectile_dodge_chance' 'add_value' (Purity 1 2 3 5 7 10))))
Add-Gem 'core' 'analyst' 'Analyst Gem' (New-Gem $flatW @(
    (GemLine 'helmet' 'tensura:analysis_level' 'add_value' (Purity 1 1 1 2 2 3)),
    (GemLine 'boots' 'tensura:analysis_distance' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'tensura:presence_sense_radius' 'add_value' (Purity 2 4 6 9 12 16))))
Add-Gem 'core' 'greedy' 'Greedy Gem' (New-Gem $flatW @(
    (GemLine 'tool' 'apothic_attributes:mining_speed' 'add_multiplied_total' (Purity 0.05 0.1 0.15 0.22 0.3 0.4)),
    (GemLine 'helmet' 'apothic_attributes:experience_gained' 'add_multiplied_total' (Purity 0.05 0.08 0.12 0.18 0.25 0.35)),
    (GemLine 'body' 'apothic_attributes:cooldown_reduction' 'add_value' (Purity 0.01 0.015 0.02 0.03 0.04 0.05))))
Add-Gem 'core' 'swift' 'Swift Gem' (New-Gem $flatW @(
    (GemLine 'body' 'tensura:chant_speed' 'add_multiplied_total' (Purity 0.03 0.05 0.08 0.12 0.16 0.22)),
    (GemLine 'weapon' 'tensura:magic_cost_multiplier' 'add_value' (Purity (-0.05) (-0.07) (-0.09) (-0.12) (-0.15) (-0.18))),
    (GemLine 'bow' 'apothic_attributes:draw_speed' 'add_multiplied_total' (Purity 0.05 0.08 0.12 0.16 0.22 0.3))))

Add-Gem 'dragonscale' 'flame' 'Flame Dragonscale' (New-Gem $uniW @(
    (GemLine 'weapon' 'apothic_attributes:fire_damage' 'add_value' (Purity 2.5 3.5 5 6.5 8 10)),
    (GemLine 'body' 'tensura:flame_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'boots' 'tensura:flame_resistance' 'add_value' (Purity 2 4 6 9 12 16))) 'flawed' $true)
Add-Gem 'dragonscale' 'frost' 'Frost Dragonscale' (New-Gem $uniW @(
    (GemLine 'weapon' 'apothic_attributes:cold_damage' 'add_value' (Purity 2.5 3.5 5 6.5 8 10)),
    (GemLine 'body' 'tensura:water_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'boots' 'tensura:water_resistance' 'add_value' (Purity 2 4 6 9 12 16))) 'flawed' $true)
Add-Gem 'dragonscale' 'storm' 'Storm Dragonscale' (New-Gem $uniW @(
    (GemLine 'weapon' 'tensura:lightning_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    # Tensura has no lightning_resistance attribute - space resistance stands
    # in for the storm's dimensional gales.
    (GemLine 'body' 'tensura:space_resistance' 'add_value' (Purity 2 4 6 9 12 16)),
    (GemLine 'boots' 'tensura:wind_resistance' 'add_value' (Purity 2 4 6 9 12 16))) 'flawed' $true)
Add-Gem 'holy' 'core' 'Holy Core' (New-Gem $uniW @(
    (GemLine 'weapon' 'tensura:light_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'apothic_attributes:healing_received' 'add_multiplied_total' (Purity 0.05 0.08 0.12 0.16 0.2 0.26)),
    (GemLine 'helmet' 'tensura:light_resistance' 'add_value' (Purity 3 5 8 12 16 22))) 'flawed' $true)
Add-Gem 'spirit' 'core' 'Spirit Core' (New-Gem $uniW @(
    (GemLine 'body' 'tensura:magic_barrier' 'add_value' (Purity 3 5 7 10 13 17)),
    (GemLine 'shield' 'tensura:physical_barrier' 'add_value' (Purity 3 5 7 10 13 17)),
    (GemLine 'helmet' 'tensura:magic_resistance' 'add_value' (Purity 2 4 6 9 12 16))) 'flawed' $true)
Add-Gem 'ultima' 'core' 'Ultima Gem' (New-Gem $uniW @(
    (GemLine 'weapon' 'apothic_attributes:crit_chance' 'add_value' (Purity 0.1 0.14 0.18 0.24 0.3 0.38)),
    (GemLine 'body' 'tensura:magic_barrier' 'add_value' (Purity 5 7 9 12 15 19)),
    (GemLine 'helmet' 'tensura:ability_mastery_gain' 'add_value' (Purity 0.1 0.15 0.2 0.3 0.4 0.55)),
    (GemLine 'shield' 'apothic_attributes:cooldown_reduction' 'add_value' (Purity 0.02 0.025 0.03 0.04 0.05 0.06))) 'flawless' $true)

# ======================================================================
# EMIT
# ======================================================================
foreach ($a in $affixes) {
    WriteJsonFile (Join-Path $data ("affixes\" + ($a.rel -replace '/', '\') + '.json')) $a.json
    $lang["affix.tensura_apotheosis:$($a.rel)"] = $a.name
    $lang["affix.tensura_apotheosis:$($a.rel).suffix"] = $a.suffix
}
foreach ($g in $gems) {
    WriteJsonFile (Join-Path $data ("gems\" + ($g.rel -replace '/', '\') + '.json')) $g.json
    # Gem model: Apotheosis resolves models as apotheosis:item/gems/<id path>
    WriteJsonFile (Join-Path $apoAssets ("models\item\gems\" + ($g.rel -replace '/', '\') + '.json')) `
        ([pscustomobject][ordered]@{
            parent   = 'item/generated'
            textures = [pscustomobject]@{ layer0 = "tensura_apotheosis:items/gems/$($g.name)" }
        })
    $lang["item.apotheosis.gem.tensura_apotheosis:$($g.rel)"] = $g.display
}

New-Item -ItemType Directory -Path (Join-Path $assets 'lang') -Force | Out-Null
[System.IO.File]::WriteAllText((Join-Path $assets 'lang\en_us.json'), ($lang | ConvertTo-Json -Depth 5), $utf8)

"affixes written: $($affixes.Count)"
"gems written:    $($gems.Count)"
"lang keys:       $($lang.Count)"
