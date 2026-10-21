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
Add-Affix 'ranged/mob_effect/venomous' 'Venomous' 'of the Poison Breath' (
    New-ProcAffix 'tensura:magicule_poison' 'arrow_target' $BOW $rangedDot)

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
$restoVals = [ordered]@{
    $R = [pscustomobject]@{ duration = 80; amplifier = 0; cooldown = 220 }
    $E = [pscustomobject]@{ duration = 110; amplifier = 0; cooldown = 200 }
    $M = [pscustomobject]@{ duration = 140; amplifier = (Range 0 1 0.5); cooldown = 180 }
}
Add-Affix 'armor/mob_effect/restorative' 'Restorative' 'of Renewal' (
    New-ProcAffix 'tensura:instant_regeneration' 'hurt_self' $BODY $restoVals 25 @() $false)

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
$reinforceVals = [ordered]@{
    $R = [pscustomobject]@{ duration = 100; amplifier = 0; cooldown = 200 }
    $E = [pscustomobject]@{ duration = 140; amplifier = 0; cooldown = 180 }
    $M = [pscustomobject]@{ duration = 180; amplifier = (Range 0 1 0.5); cooldown = 160 }
}
Add-Affix 'shield/mob_effect/reinforced' 'Reinforced' 'of Endurance' (
    New-ProcAffix 'tensura:reinforcement' 'hurt_self' $SHLD $reinforceVals 25 @() $false)

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
$webVals = [ordered]@{
    $R = [pscustomobject]@{ duration = (Range 60 80 20); amplifier = 0; cooldown = 160 }
    $E = [pscustomobject]@{ duration = (Range 80 100 20); amplifier = 0; cooldown = 150 }
    $M = [pscustomobject]@{ duration = (Range 100 140 20); amplifier = (Range 0 1 0.5); cooldown = 140 }
}
Add-Affix 'generic/mob_effect/entangling' 'Entangling' 'of the Spider' (
    New-ProcAffix 'tensura:webbed' 'attack_target' $ALL $webVals 20)

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

$gems = @()   # @{ rel; name; tex; display; json }
function Add-Gem([string]$family, [string]$name, [string]$display, $json, [string]$tex = '') {
    # Project convention: never generate textures. The original 14 gems keep their
    # own PNGs; additional gems pass $tex = an existing Apotheosis gem texture id
    # as a placeholder until hand-drawn art arrives.
    if (-not $tex) { $tex = "tensura_apotheosis:items/gems/$name" }
    $script:gems += @{ rel = "$family/$name"; name = $name; tex = $tex; display = $display; json = $json }
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
    (GemLine 'helmet' 'tensura:light_resistance' 'add_value' (Purity 3 5 8 12 16 22))) 'flawed' $true) 'tensura_apotheosis:items/gems/holy'
Add-Gem 'spirit' 'core' 'Spirit Core' (New-Gem $uniW @(
    (GemLine 'body' 'tensura:magic_barrier' 'add_value' (Purity 3 5 7 10 13 17)),
    (GemLine 'shield' 'tensura:physical_barrier' 'add_value' (Purity 3 5 7 10 13 17)),
    (GemLine 'helmet' 'tensura:magic_resistance' 'add_value' (Purity 2 4 6 9 12 16))) 'flawed' $true) 'tensura_apotheosis:items/gems/spirit'
Add-Gem 'ultima' 'core' 'Ultima Gem' (New-Gem $uniW @(
    (GemLine 'weapon' 'apothic_attributes:crit_chance' 'add_value' (Purity 0.1 0.14 0.18 0.24 0.3 0.38)),
    (GemLine 'body' 'tensura:magic_barrier' 'add_value' (Purity 5 7 9 12 15 19)),
    (GemLine 'helmet' 'tensura:ability_mastery_gain' 'add_value' (Purity 0.1 0.15 0.2 0.3 0.4 0.55)),
    (GemLine 'shield' 'apothic_attributes:cooldown_reduction' 'add_value' (Purity 0.02 0.025 0.03 0.04 0.05 0.06))) 'flawless' $true) 'tensura_apotheosis:items/gems/ultima'

# Additional gems. Textures reuse Apotheosis' stock gem art as placeholders -
# textures are never generated (hand-drawn art is planned later).
Add-Gem 'core' 'barrier' 'Barrier Gem' (New-Gem $flatW @(
    (GemLine 'body' 'tensura:multilayer_barrier' 'add_value' (Purity 1 1 2 2 3 4)),
    (GemLine 'shield' 'tensura:magic_barrier' 'add_value' (Purity 2 4 6 9 12 16)))) 'apotheosis:items/gems/guardian'
Add-Gem 'core' 'shadow' 'Shadow Gem' (New-Gem $flatW @(
    (GemLine 'helmet' 'tensura:presence_concealment' 'add_value' (Purity 3 5 8 11 15 20)),
    (GemLine 'boots' 'tensura:dodge_strength' 'add_value' (Purity 1 2 3 5 7 10)))) 'apotheosis:items/gems/endersurge'
Add-Gem 'core' 'vigil' 'Vigil Gem' (New-Gem $flatW @(
    (GemLine 'helmet' 'tensura:heat_sense_radius' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'tensura:presence_sense_radius' 'add_value' (Purity 2 4 6 9 12 16)))) 'apotheosis:items/gems/inferno'
Add-Gem 'core' 'stalwart' 'Stalwart Gem' (New-Gem $flatW @(
    (GemLine 'body' 'minecraft:generic.armor' 'add_value' (Purity 2 3 4 5 6 8)),
    (GemLine 'boots' 'minecraft:generic.armor_toughness' 'add_value' (Purity 1 2 3 4 5 7)))) 'apotheosis:items/gems/ballast'
Add-Gem 'core' 'gale' 'Gale Gem' (New-Gem $flatW @(
    (GemLine 'boots' 'minecraft:generic.movement_speed' 'add_multiplied_total' (Purity 0.02 0.03 0.04 0.06 0.08 0.1)),
    (GemLine 'body' 'tensura:wind_resistance' 'add_value' (Purity 1 2 3 4 6 8)))) 'apotheosis:items/gems/slipstream'
Add-Gem 'core' 'earthen' 'Earthen Gem' (New-Gem $flatW @(
    (GemLine 'body' 'minecraft:generic.knockback_resistance' 'add_value' (Purity 0.05 0.1 0.15 0.2 0.3 0.4)),
    (GemLine 'boots' 'tensura:earth_resistance' 'add_value' (Purity 2 4 6 9 12 16)))) 'apotheosis:items/gems/verdant_ruin'
Add-Gem 'core' 'fury' 'Fury Gem' (New-Gem $flatW @(
    (GemLine 'weapon' 'minecraft:generic.attack_damage' 'add_multiplied_total' (Purity 0.03 0.05 0.07 0.1 0.13 0.17)),
    (GemLine 'bow' 'apothic_attributes:arrow_damage' 'add_multiplied_total' (Purity 0.04 0.06 0.09 0.12 0.16 0.2)))) 'apotheosis:items/gems/tyrannical'
Add-Gem 'core' 'vigor' 'Vigor Gem' (New-Gem $flatW @(
    (GemLine 'body' 'minecraft:generic.max_health' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'helmet' 'tensura:spiritual_health_regeneration' 'add_value' (Purity 0.1 0.2 0.3 0.5 0.7 1)))) 'apotheosis:items/gems/solar'
Add-Gem 'core' 'juggernaut' 'Juggernaut Gem' (New-Gem $flatW @(
    (GemLine 'body' 'minecraft:generic.knockback_resistance' 'add_value' (Purity 0.1 0.15 0.2 0.3 0.4 0.5)),
    (GemLine 'boots' 'minecraft:generic.armor' 'add_value' (Purity 1 2 3 4 5 7)))) 'apotheosis:items/gems/warlord'
Add-Gem 'core' 'striker' 'Striker Gem' (New-Gem $flatW @(
    (GemLine 'weapon' 'minecraft:generic.attack_damage' 'add_multiplied_total' (Purity 0.04 0.06 0.08 0.11 0.14 0.18)),
    (GemLine 'bow' 'apothic_attributes:arrow_damage' 'add_multiplied_total' (Purity 0.05 0.07 0.1 0.13 0.17 0.21)))) 'apotheosis:items/gems/combatant'
Add-Gem 'core' 'precision' 'Precision Gem' (New-Gem $flatW @(
    (GemLine 'bow' 'apothic_attributes:crit_chance' 'add_value' (Purity 0.05 0.07 0.1 0.13 0.16 0.2)),
    (GemLine 'weapon' 'apothic_attributes:armor_pierce' 'add_value' (Purity 2 3 4 6 8 10)))) 'apotheosis:items/gems/breach'
Add-Gem 'core' 'sprinter' 'Sprinter Gem' (New-Gem $flatW @(
    (GemLine 'boots' 'minecraft:generic.movement_speed' 'add_multiplied_total' (Purity 0.01 0.02 0.03 0.04 0.06 0.08)),
    (GemLine 'body' 'minecraft:generic.safe_fall_distance' 'add_value' (Purity 2 3 4 6 8 11)))) 'apotheosis:items/gems/samurai'
Add-Gem 'core' 'brawler' 'Brawler Gem' (New-Gem $flatW @(
    (GemLine 'weapon' 'minecraft:generic.attack_speed' 'add_value' (Purity 0.05 0.08 0.1 0.14 0.18 0.22)),
    (GemLine 'helmet' 'minecraft:generic.armor_toughness' 'add_value' (Purity 1 2 3 4 5 7)))) 'apotheosis:items/gems/brawlers'
Add-Gem 'core' 'mystic' 'Mystic Gem' (New-Gem $flatW @(
    (GemLine 'helmet' 'tensura:magic_resistance' 'add_value' (Purity 2 3 5 7 9 12)),
    (GemLine 'body' 'tensura:magic_barrier' 'add_value' (Purity 2 3 5 7 9 12)))) 'apotheosis:items/gems/mageslayer'
Add-Gem 'core' 'executioner' 'Executioner Gem' (New-Gem $flatW @(
    (GemLine 'weapon' 'apothic_attributes:current_hp_damage' 'add_value' (Purity 0.04 0.06 0.09 0.12 0.15 0.19)),
    (GemLine 'bow' 'apothic_attributes:crit_damage' 'add_value' (Purity 0.08 0.11 0.14 0.18 0.22 0.27)))) 'apotheosis:items/gems/blood_lord'

Add-Gem 'dragonscale' 'earth' 'Earth Dragonscale' (New-Gem $uniW @(
    (GemLine 'weapon' 'tensura:earth_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'tensura:earth_resistance' 'add_value' (Purity 2 4 6 9 12 16)),
    (GemLine 'helmet' 'minecraft:generic.armor' 'add_value' (Purity 2 3 4 5 6 8))) 'flawed' $true) 'apotheosis:items/gems/earth'
Add-Gem 'dragonscale' 'darkness' 'Darkness Dragonscale' (New-Gem $uniW @(
    (GemLine 'weapon' 'tensura:darkness_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'tensura:darkness_resistance' 'add_value' (Purity 2 4 6 9 12 16)),
    (GemLine 'helmet' 'tensura:dark_vision' 'add_value' (Purity 1 1 2 2 3 4))) 'flawed' $true) 'apotheosis:items/gems/queen'
Add-Gem 'dragonscale' 'wind' 'Wind Dragonscale' (New-Gem $uniW @(
    (GemLine 'weapon' 'tensura:wind_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'tensura:wind_resistance' 'add_value' (Purity 2 4 6 9 12 16)),
    (GemLine 'boots' 'minecraft:generic.movement_speed' 'add_multiplied_total' (Purity 0.02 0.03 0.05 0.07 0.09 0.12))) 'flawed' $true) 'apotheosis:items/gems/lightning'
Add-Gem 'dragonscale' 'gravity' 'Gravity Dragonscale' (New-Gem $uniW @(
    (GemLine 'weapon' 'tensura:gravity_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'minecraft:generic.max_health' 'add_value' (Purity 5 8 12 16 22 30)),
    (GemLine 'boots' 'minecraft:generic.knockback_resistance' 'add_value' (Purity 0.05 0.1 0.15 0.2 0.3 0.4))) 'flawed' $true) 'apotheosis:items/gems/lunar'
Add-Gem 'holy' 'judgment' 'Judgment Core' (New-Gem $uniW @(
    (GemLine 'bow' 'tensura:light_boost' 'add_value' (Purity 3 5 8 12 16 22)),
    (GemLine 'body' 'tensura:light_resistance' 'add_value' (Purity 2 4 6 9 12 16))) 'flawed' $true) 'apotheosis:items/gems/splendor'
Add-Gem 'spirit' 'guardian' 'Guardian Core' (New-Gem $uniW @(
    (GemLine 'shield' 'minecraft:generic.armor' 'add_value' (Purity 2 3 4 6 8 10)),
    (GemLine 'body' 'minecraft:generic.max_health' 'add_value' (Purity 5 8 12 16 22 30))) 'flawed' $true) 'apotheosis:items/gems/molten_breach'
Add-Gem 'spirit' 'mender' 'Mender Core' (New-Gem $uniW @(
    (GemLine 'body' 'apothic_attributes:healing_received' 'add_multiplied_total' (Purity 0.04 0.06 0.09 0.12 0.16 0.2)),
    (GemLine 'helmet' 'tensura:spiritual_health_regeneration' 'add_value' (Purity 0.15 0.3 0.45 0.75 1 1.5))) 'flawed' $true) 'apotheosis:items/gems/forest'
Add-Gem 'ultima' 'sovereign' 'Sovereign Gem' (New-Gem $uniW @(
    (GemLine 'weapon' 'apothic_attributes:crit_damage' 'add_value' (Purity 0.12 0.16 0.2 0.26 0.32 0.4)),
    (GemLine 'body' 'tensura:max_magicule' 'add_value' (Purity 15 25 40 60 90 130)),
    (GemLine 'shield' 'apothic_attributes:cooldown_reduction' 'add_value' (Purity 0.02 0.025 0.03 0.04 0.05 0.06))) 'flawless' $true) 'apotheosis:items/gems/royalty'

# ======================================================================
# ELITE BOSS GEAR SETS + ELITES
# ======================================================================
# Gear sets are Placebo weighted-dynamic-registry entries. Their "tags" are
# plain strings: an elite's valid_gear_sets "#foo" matches any gear set whose
# tags contain "foo". Two tracks:
#   Track A - tags collide with Apotheosis' vanilla pools (haven_melee, ...
#             pinnacle_ranged) so the 4 built-in elites can roll Tensura gear.
#   Track B - unique "tensura_<theme>_<tier>" tags, referenced only by our 38
#             Tensura elites, so themed gear never leaks onto vanilla bosses.
# Elites inherit Tensura's exact spawn logic: constraints.biomes is the literal
# "#tensura:<mob>_spawn" biome tag Tensura gates that mob's natural spawns by.
Add-Type -AssemblyName System.IO.Compression.FileSystem

$gearsets = @()   # @{ rel; json }
$elites   = @()   # @{ rel; json; mob; display }
function Add-GearSet([string]$rel, $json) {
    $script:gearsets += @{ rel = $rel; json = $json }
}
function Add-Elite([string]$rel, [string]$mob, [string]$display, $json) {
    $script:elites += @{ rel = $rel; mob = $mob; display = $display; json = $json }
    $script:lang["elite.tensura_apotheosis.$mob"] = "Elite $display"
}

function WS($id, [int]$w = 10) {
    [pscustomobject][ordered]@{ stack = [pscustomobject][ordered]@{ count = 1; id = $id }; weight = $w }
}
# armor ids for a material; leather uses monster_leather_<slot>_<grade>,
# every other line uses <mat>_<slot>.
function ArmorIds($armor) {
    if ($armor.grade) {
        $p = 'monster_leather'
        @{ h = "tensura:${p}_helmet_$($armor.grade)"; c = "tensura:${p}_chestplate_$($armor.grade)"
           l = "tensura:${p}_leggings_$($armor.grade)"; b = "tensura:${p}_boots_$($armor.grade)" }
    } else {
        $m = $armor.mat
        @{ h = "tensura:${m}_helmet"; c = "tensura:${m}_chestplate"
           l = "tensura:${m}_leggings"; b = "tensura:${m}_boots" }
    }
}
function New-GearSetJson($tags, $mainhands, $armor, [string]$barding, [int]$weight) {
    $o = [ordered]@{ type = 'placebo:gear_set' }
    if ($mainhands -and $mainhands.Count) { $o['mainhands'] = @($mainhands | ForEach-Object { WS $_ }) }
    if ($barding) {
        # AnimalArmorItem horse barding occupies the chest slot; only mobs whose
        # renderer has an *ArmorLayer (pegasus/unicorn/hover_lizard) show it.
        $o['chestplates'] = @(WS "minecraft:${barding}_horse_armor")
    } elseif ($armor) {
        $ids = ArmorIds $armor
        $o['helmets']      = @(WS $ids.h)
        $o['chestplates']  = @(WS $ids.c)
        $o['leggings']     = @(WS $ids.l)
        $o['boots']        = @(WS $ids.b)
    }
    $o['tags']   = @($tags)
    $o['weight'] = $weight   # selection weight within the tag's pool (required)
    [pscustomobject]$o
}

$tiers = @('haven', 'frontier', 'ascent', 'summit', 'pinnacle')

# ---- Track A: Tensura gear injected into Apotheosis' vanilla tier pools -----
# The 4 built-in elites (craig, honeyed_archer, undead_knight, withering_archer)
# reference these tags, so Tensura equipment joins their loadout alongside the
# existing vanilla sets. Melee weapons drop on kill; armor is applied as stats.
$trackAArmor = @(
    @{ mat = 'leather'; grade = 'd' }, @{ mat = 'leather'; grade = 'c' },
    @{ mat = 'leather'; grade = 'b' }, @{ mat = 'leather'; grade = 'a' },
    @{ mat = 'orichalcum' })
$trackAMelee = @(
    @('tensura:wooden_katana', 'tensura:wooden_great_sword'),
    @('tensura:iron_katana', 'tensura:iron_great_sword'),
    @('tensura:low_magisteel_katana', 'tensura:low_magisteel_spear'),
    @('tensura:high_magisteel_katana', 'tensura:mithril_great_sword'),
    @('tensura:orichalcum_katana', 'tensura:hihiirokane_great_sword'))
$trackARanged = @(
    @('tensura:short_bow'),
    @('tensura:short_bow', 'tensura:long_bow'),
    @('tensura:long_bow'),
    @('tensura:war_bow', 'tensura:spider_bow'),
    @('tensura:war_bow', 'tensura:war_spider_bow', 'tensura:ant_crossbow'))
# Selection weight within each tier pool; mirrors Apotheosis' own ladder (haven
# 100 down to pinnacle 40) so Tensura gear shows up alongside vanilla without
# swamping it.
$trackAWeight = @(100, 90, 70, 50, 40)
for ($i = 0; $i -lt $tiers.Count; $i++) {
    $t = $tiers[$i]
    Add-GearSet "$t/melee/tensura" (New-GearSetJson @("${t}_melee") $trackAMelee[$i] $trackAArmor[$i] '' $trackAWeight[$i])
    Add-GearSet "$t/ranged/tensura" (New-GearSetJson @("${t}_ranged") $trackARanged[$i] $trackAArmor[$i] '' $trackAWeight[$i])
}

# ---- Track B: identity-themed sets for the 38 Tensura elites ----------------
# 14 themes. armor[i] and weapons[i] index the 5 tiers (haven..pinnacle). Barding
# themes override the chest slot with horse armor (visible on their 3 renderable
# mobs). Per-theme stat bands feed the elite blocks below.
$themes = @(
    @{ key = 'demon'
       armor = @(@{mat='leather';grade='d'}, @{mat='leather';grade='c'}, @{mat='pure_magisteel'}, @{mat='orichalcum'}, @{mat='hihiirokane'})
       weapons = @(@('tensura:low_magic_staff'), @('tensura:low_magic_staff','tensura:iron_katana'), @('tensura:medium_magic_staff','tensura:low_magisteel_katana'), @('tensura:high_magic_staff','tensura:high_magisteel_katana'), @('tensura:grimoire_a','tensura:grimoire_special_a','tensura:orichalcum_katana'))
       barding=''; effects=@('minecraft:fire_resistance','minecraft:night_vision')
       hp=@(40,70); dmg=@(0.4,0.7); scale=@(1.2,1.5); ench=0.4; success=0.01 }
    @{ key = 'salamander'
       armor = @(@{mat='leather';grade='d'}, @{mat='leather';grade='c'}, @{mat='high_magisteel'}, @{mat='adamantite'}, @{mat='hihiirokane'})
       weapons = @(@('tensura:wooden_scythe'), @('tensura:iron_scythe'), @('tensura:low_magisteel_scythe'), @('tensura:adamantite_scythe'), @('tensura:hihiirokane_scythe','tensura:blade_tiger_scythe'))
       barding=''; effects=@('minecraft:fire_resistance')
       hp=@(35,60); dmg=@(0.35,0.6); scale=@(1.1,1.3); ench=0.35; success=0.012 }
    @{ key = 'scaled'
       armor = @(@{mat='leather';grade='d'}, @{mat='leather';grade='c'}, @{mat='serpent_scalemail'}, @{mat='serpent_scalemail'}, @{mat='charybdis_scalemail'})
       weapons = @(@('tensura:wooden_spear'), @('tensura:iron_spear'), @('tensura:low_magisteel_spear'), @('tensura:mithril_spear'), @('tensura:orichalcum_spear','tensura:vortex_spear'))
       barding=''; effects=@('minecraft:fire_resistance')
       hp=@(45,80); dmg=@(0.4,0.75); scale=@(1.3,1.7); ench=0.4; success=0.01 }
    @{ key = 'lizard'
       armor = @(@{mat='leather';grade='d'}, @{mat='leather';grade='c'}, @{mat='serpent_scalemail'}, @{mat='serpent_scalemail'}, @{mat='serpent_scalemail'})
       weapons = @(@('tensura:wooden_short_sword'), @('tensura:iron_short_sword'), @('tensura:low_magisteel_short_sword'), @('tensura:pure_magisteel_kodachi'), @('tensura:orichalcum_kodachi'))
       barding=''; effects=@()
       hp=@(25,45); dmg=@(0.25,0.45); scale=@(0.9,1.1); ench=0.25; success=0.015 }
    @{ key = 'hover'
       armor = @()
       weapons = @(@('tensura:wooden_short_sword'), @('tensura:iron_short_sword'), @('tensura:low_magisteel_short_sword'), @('tensura:pure_magisteel_kodachi'), @('tensura:orichalcum_kodachi'))
       barding='iron'; effects=@()
       hp=@(25,45); dmg=@(0.25,0.45); scale=@(0.9,1.1); ench=0.25; success=0.015 }
    @{ key = 'spider'
       armor = @(@{mat='leather';grade='c'}, @{mat='leather';grade='c'}, @{mat='knight_spider_carapace'}, @{mat='knight_spider_carapace'}, @{mat='knight_spider_carapace'})
       weapons = @(@('tensura:spider_dagger','tensura:short_bow'), @('tensura:spider_dagger','tensura:short_bow'), @('tensura:spider_dagger','tensura:spider_bow'), @('tensura:spider_dagger','tensura:long_spider_bow'), @('tensura:spider_dagger','tensura:war_spider_bow'))
       barding=''; effects=@('minecraft:night_vision')
       hp=@(30,55); dmg=@(0.3,0.55); scale=@(1.0,1.3); ench=0.3; success=0.012 }
    @{ key = 'insect'
       armor = @(@{mat='leather';grade='c'}, @{mat='leather';grade='c'}, @{mat='ant_carapace'}, @{mat='ant_carapace'}, @{mat='ant_carapace'})
       weapons = @(@('tensura:short_bow'), @('tensura:short_bow','tensura:centipede_dagger'), @('tensura:ant_crossbow','tensura:centipede_dagger'), @('tensura:ant_crossbow','tensura:centipede_dagger'), @('tensura:ant_crossbow','tensura:centipede_dagger'))
       barding=''; effects=@()
       hp=@(28,50); dmg=@(0.28,0.5); scale=@(1.0,1.2); ench=0.28; success=0.014 }
    @{ key = 'beast'
       armor = @(@{mat='leather';grade='d'}, @{mat='leather';grade='c'}, @{mat='leather';grade='b'}, @{mat='leather';grade='a'}, @{mat='leather';grade='special_a'})
       weapons = @(@('tensura:wooden_great_sword'), @('tensura:iron_great_sword'), @('tensura:beast_horn_spear','tensura:low_magisteel_great_sword'), @('tensura:blade_tiger_scythe','tensura:mithril_great_sword'), @('tensura:adamantite_great_sword','tensura:blade_tiger_scythe'))
       barding=''; effects=@()
       hp=@(35,65); dmg=@(0.35,0.6); scale=@(1.1,1.4); ench=0.35; success=0.012 }
    @{ key = 'aquatic'
       armor = @(@{mat='leather';grade='c'}, @{mat='leather';grade='c'}, @{mat='serpent_scalemail'}, @{mat='serpent_scalemail'}, @{mat='serpent_scalemail'})
       weapons = @(@('tensura:wooden_spear'), @('tensura:iron_spear'), @('tensura:low_magisteel_spear'), @('tensura:silver_spear'), @('tensura:orichalcum_spear'))
       barding=''; effects=@('minecraft:water_breathing')
       hp=@(30,55); dmg=@(0.3,0.5); scale=@(1.1,1.4); ench=0.3; success=0.014 }
    @{ key = 'armorsaurus'
       armor = @(@{mat='leather';grade='b'}, @{mat='leather';grade='b'}, @{mat='armorsaurus_scalemail'}, @{mat='armorsaurus_scalemail'}, @{mat='armorsaurus_scalemail'})
       weapons = @(@('tensura:iron_great_sword'), @('tensura:iron_great_sword'), @('tensura:adamantite_great_sword'), @('tensura:adamantite_great_sword'), @('tensura:adamantite_great_sword','tensura:hihiirokane_great_sword'))
       barding=''; effects=@('minecraft:fire_resistance')
       hp=@(50,90); dmg=@(0.45,0.8); scale=@(1.4,1.8); ench=0.45; success=0.008 }
    @{ key = 'slime'
       armor = @(@{mat='leather';grade='c'}, @{mat='leather';grade='c'}, @{mat='pure_magisteel'}, @{mat='pure_magisteel'}, @{mat='orichalcum'})
       weapons = @(@('tensura:slime_staff'), @('tensura:slime_staff'), @('tensura:slime_staff'), @('tensura:slime_staff'), @('tensura:slime_staff','tensura:high_magic_staff'))
       barding=''; effects=@()
       hp=@(40,75); dmg=@(0.3,0.55); scale=@(1.2,1.6); ench=0.3; success=0.01 }
    @{ key = 'spore'
       armor = @(@{mat='leather';grade='c'}, @{mat='leather';grade='c'}, @{mat='mithril'}, @{mat='mithril'}, @{mat='orichalcum'})
       weapons = @(@('tensura:low_magic_staff'), @('tensura:low_magic_staff'), @('tensura:medium_magic_staff'), @('tensura:high_magic_staff'), @('tensura:grimoire_b'))
       barding=''; effects=@('minecraft:regeneration')
       hp=@(30,55); dmg=@(0.3,0.5); scale=@(1.0,1.2); ench=0.3; success=0.014 }
    @{ key = 'orc'
       armor = @(@{mat='leather';grade='c'}, @{mat='leather';grade='c'}, @{mat='silver'}, @{mat='adamantite'}, @{mat='adamantite'})
       weapons = @(@('tensura:wooden_great_sword'), @('tensura:iron_great_sword'), @('tensura:iron_great_sword','tensura:golden_great_sword'), @('tensura:adamantite_great_sword'), @('tensura:adamantite_great_sword','tensura:orichalcum_great_sword'))
       barding=''; effects=@()
       hp=@(45,80); dmg=@(0.45,0.75); scale=@(1.3,1.6); ench=0.4; success=0.01 }
    @{ key = 'unicorn'
       armor = @(@{mat='leather';grade='b'}, @{mat='leather';grade='b'}, @{mat='pure_magisteel'}, @{mat='mithril'}, @{mat='orichalcum'})
       weapons = @(@('tensura:wooden_spear'), @('tensura:iron_spear'), @('tensura:unicorn_horn_spear'), @('tensura:unicorn_horn_spear','tensura:mithril_spear'), @('tensura:unicorn_horn_spear','tensura:orichalcum_spear'))
       barding='iron'; effects=@('minecraft:fire_resistance')
       hp=@(40,70); dmg=@(0.4,0.65); scale=@(1.2,1.5); ench=0.4; success=0.01 }
)
$themeByKey = @{}
foreach ($th in $themes) { $themeByKey[$th.key] = $th }

# Barding tiers by world tier for the two barding themes (iron..diamond).
$bardingByTier = @('iron', 'iron', 'iron', 'golden', 'diamond')

foreach ($th in $themes) {
    for ($i = 0; $i -lt $tiers.Count; $i++) {
        $t = $tiers[$i]
        $barding = ''
        if ($th.barding) { $barding = $bardingByTier[$i] }
        $armorArg = $null
        if (-not $barding -and $th.armor -and $th.armor.Count -gt $i) { $armorArg = $th.armor[$i] }
        # Each themed tag resolves to exactly one set, so weight is cosmetic.
        Add-GearSet "themes/$($th.key)/$t" (New-GearSetJson @("tensura_$($th.key)_$t") $th.weapons[$i] $armorArg $barding 100)
    }
}

# ---- The 38 elites ----------------------------------------------------------
# Every mob here has a #tensura:<mob>_spawn biome tag (asserted below), so its
# elite inherits Tensura's exact natural-spawn biomes via constraints.biomes.
# Weights ladder from common (haven) to rare (pinnacle); success_chance is the
# per-spawn chance a natural spawn of the base mob becomes this elite.
$spawnTags = New-Object 'System.Collections.Generic.HashSet[string]'
$tj = [System.IO.Compression.ZipFile]::OpenRead('C:\Games\Minecraft\Mods\tensura-apotheosis\libs\tensura-neoforge-2.0.1.3.jar')
foreach ($en in $tj.Entries) {
    if ($en.FullName -like 'data/tensura/tags/worldgen/biome/*_spawn.json') {
        [void]$spawnTags.Add(([System.IO.Path]::GetFileNameWithoutExtension($en.FullName) -replace '_spawn$', ''))
    }
}
$tj.Dispose()

# mob -> theme + display name. World tiers are NOT assigned here: they are
# computed below from each theme's actual combat stats (hp, damage, scale,
# enchant chance, success chance), so banding follows the numbers - and
# identical stat profiles (lizard == hover, scaled == orc on composite)
# always land in the same band.
$roster = @(
    @{ mob='arch_daemon';       theme='demon';      display='Arch Daemon' }
    @{ mob='greater_daemon';    theme='demon';      display='Greater Daemon' }
    @{ mob='lesser_daemon';     theme='demon';      display='Lesser Daemon' }
    @{ mob='salamander';        theme='salamander'; display='Salamander' }
    @{ mob='basilisk';          theme='scaled';     display='Basilisk' }
    @{ mob='dragon_peacock';    theme='scaled';     display='Dragon Peacock' }
    @{ mob='feathered_serpent'; theme='scaled';     display='Feathered Serpent' }
    @{ mob='tempest_serpent';   theme='scaled';     display='Tempest Serpent' }
    @{ mob='leech_lizard';      theme='lizard';     display='Leech Lizard' }
    @{ mob='hover_lizard';      theme='hover';      display='Hover Lizard' }
    @{ mob='black_spider';      theme='spider';     display='Black Spider' }
    @{ mob='knight_spider';     theme='spider';     display='Knight Spider' }
    @{ mob='army_wasp';         theme='insect';     display='Army Wasp' }
    @{ mob='giant_ant';         theme='insect';     display='Giant Ant' }
    @{ mob='evil_centipede';    theme='insect';     display='Evil Centipede' }
    @{ mob='hell_caterpillar';  theme='insect';     display='Hell Caterpillar' }
    @{ mob='hell_moth';         theme='insect';     display='Hell Moth' }
    @{ mob='barghest';          theme='beast';      display='Barghest' }
    @{ mob='beast_gnome';       theme='beast';      display='Beast Gnome' }
    @{ mob='direwolf';          theme='beast';      display='Direwolf' }
    @{ mob='blade_tiger';       theme='beast';      display='Blade Tiger' }
    @{ mob='giant_bat';         theme='beast';      display='Giant Bat' }
    @{ mob='giant_bear';        theme='beast';      display='Giant Bear' }
    @{ mob='horned_bear';       theme='beast';      display='Horned Bear' }
    @{ mob='horned_rabbit';     theme='beast';      display='Horned Rabbit' }
    @{ mob='hound_dog';         theme='beast';      display='Hound Dog' }
    @{ mob='one_eyed_owl';      theme='beast';      display='One Eyed Owl' }
    @{ mob='winged_cat';        theme='beast';      display='Winged Cat' }
    @{ mob='giant_cod';         theme='aquatic';    display='Giant Cod' }
    @{ mob='giant_salmon';      theme='aquatic';    display='Giant Salmon' }
    @{ mob='landfish';          theme='aquatic';    display='Landfish' }
    @{ mob='spear_toro';        theme='aquatic';    display='Spear Toro' }
    @{ mob='armorsaurus';       theme='armorsaurus';display='Armorsaurus' }
    @{ mob='slime';             theme='slime';      display='Slime' }
    @{ mob='phantaspore';       theme='spore';      display='Phantaspore' }
    @{ mob='orc';               theme='orc';        display='Orc' }
    @{ mob='pegasus';           theme='unicorn';    display='Pegasus' }
    @{ mob='unicorn';           theme='unicorn';    display='Unicorn' }
)
if ($roster.Count -ne 38) { throw "roster is $($roster.Count) expected 38" }

$tierIndex = @{}
for ($i = 0; $i -lt $tiers.Count; $i++) { $tierIndex[$tiers[$i]] = $i }

# ---- Stat-derived world-tier banding ----------------------------------------
# Tier bands gate elite spawns, elite affix rarities, gateway opening and
# gateway rewards (see $tierProfile). Rather than hand-assigning mobs, score
# every roster mob on its theme's actual combat stats and band by rank:
#   1. Each stat becomes an average-rank percentile over the 38-mob roster
#      (a value shared by k mobs sits at the midpoint of its rank block).
#      success_chance counts inverted - the rarer an elite is, the stronger.
#   2. Composite = mean of the five percentiles.
#   3. Themes sort strongest-first with a deterministic tiebreak chain
#      (composite, hp, dmg, scale, ench, success asc, theme key).
#   4. Greedy fill from the top: pinnacle takes the strongest themes, haven
#      the weakest; a new band opens before a theme would push the current
#      one past ~n/5 mobs, so tied stat profiles always share a band.
$mobCountByTheme = @{}
foreach ($r in $roster) { $mobCountByTheme[$r.theme] = [int]$mobCountByTheme[$r.theme] + 1 }

$rawStats = foreach ($th in $themes) {
    [pscustomobject]@{
        key   = $th.key
        mobs  = $mobCountByTheme[$th.key]
        hp    = ($th.hp[0] + $th.hp[1]) / 2
        dmg   = ($th.dmg[0] + $th.dmg[1]) / 2
        scale = ($th.scale[0] + $th.scale[1]) / 2
        ench  = $th.ench
        succ  = $th.success
    }
}
function Get-StatPercentile($values, $v) {
    $below = @($values | Where-Object { $_ -lt $v }).Count
    $equal = @($values | Where-Object { $_ -eq $v }).Count
    return ($below + 0.5 * $equal) / $values.Count
}
$allHp = [System.Collections.Generic.List[double]]::new()
$allDmg = [System.Collections.Generic.List[double]]::new()
$allScale = [System.Collections.Generic.List[double]]::new()
$allEnch = [System.Collections.Generic.List[double]]::new()
$allSucc = [System.Collections.Generic.List[double]]::new()
foreach ($p in $rawStats) {
    for ($i = 0; $i -lt $p.mobs; $i++) {
        $allHp.Add($p.hp); $allDmg.Add($p.dmg); $allScale.Add($p.scale)
        $allEnch.Add($p.ench); $allSucc.Add($p.succ)
    }
}
$scoredStats = foreach ($p in $rawStats) {
    $pctHp = Get-StatPercentile $allHp $p.hp
    $pctDmg = Get-StatPercentile $allDmg $p.dmg
    $pctScale = Get-StatPercentile $allScale $p.scale
    $pctEnch = Get-StatPercentile $allEnch $p.ench
    $pctSucc = 1 - (Get-StatPercentile $allSucc $p.succ)   # inverted: rarer = stronger
    [pscustomobject]@{
        key       = $p.key
        mobs      = $p.mobs
        composite = ($pctHp + $pctDmg + $pctScale + $pctEnch + $pctSucc) / 5
        hp        = $p.hp
        dmg       = $p.dmg
        scale     = $p.scale
        ench      = $p.ench
        succ      = $p.succ
    }
}
$orderedThemes = $scoredStats | Sort-Object `
    @{ Expression = { $_.composite }; Descending = $true },
    @{ Expression = { $_.hp };        Descending = $true },
    @{ Expression = { $_.dmg };       Descending = $true },
    @{ Expression = { $_.scale };     Descending = $true },
    @{ Expression = { $_.ench };      Descending = $true },
    @{ Expression = { $_.succ };      Descending = $false },
    @{ Expression = { $_.key };       Descending = $false }

$bandTarget = $roster.Count / $tiers.Count
$themeTier = @{}
$bandIdx = $tiers.Count - 1
$bandThemes = [System.Collections.Generic.List[string]]::new()
$bandMobCount = 0
foreach ($p in $orderedThemes) {
    if ($bandThemes.Count -gt 0 -and ($bandMobCount + $p.mobs) -gt $bandTarget -and $bandIdx -gt 0) {
        foreach ($k in $bandThemes) { $themeTier[$k] = $tiers[$bandIdx] }
        $bandIdx--
        $bandThemes.Clear()
        $bandMobCount = 0
    }
    $bandThemes.Add($p.key)
    $bandMobCount += $p.mobs
}
if ($bandIdx -ne 0) {
    throw "stat banding left world tier(s) above '$($tiers[$bandIdx])' empty - stats too clumped?"
}
foreach ($k in $bandThemes) { $themeTier[$k] = $tiers[0] }
foreach ($r in $roster) { $r.tier = $themeTier[$r.theme] }
Write-Output 'tier bands (stat-derived):'
foreach ($t in $tiers) {
    $mobs = @($roster | Where-Object { $_.tier -eq $t } | ForEach-Object { $_.mob })
    Write-Output ("  {0,-9} ({1,2})  {2}" -f $t, $mobs.Count, ($mobs -join ' '))
}

# World-tier profiles: one table drives BOTH the elite affix ladder and the
# gateway reward ladder, so gear rarity and gem quality rise together (the
# "rewards scale with tier" contract). Elite bands follow Apotheosis' own
# rarity weights per world tier (common is haven-heavy, mythic only exists
# from summit on); summit/pinnacle elites share the {epic, mythic} pool and
# separate on affix_chance instead, since mythic is the cap. Band MEMBERSHIP
# is stat-derived by the block above, never hand-picked.
$tierProfile = [ordered]@{
    haven    = [pscustomobject][ordered]@{ eliteRarities = @('apotheosis:common', 'apotheosis:uncommon'); affixChance = 0.45; rewardRarities = @('apotheosis:common', 'apotheosis:uncommon'); purities = @('cracked', 'normal');        dust = 8;  heraldXp = 100; bossXp = 300; finalXp = 500;  stackCount = 8  }
    frontier = [pscustomobject][ordered]@{ eliteRarities = @('apotheosis:uncommon', 'apotheosis:rare');  affixChance = 0.5;  rewardRarities = @('apotheosis:uncommon', 'apotheosis:rare');  purities = @('normal');                 dust = 12; heraldXp = 150; bossXp = 400; finalXp = 650;  stackCount = 10 }
    ascent   = [pscustomobject][ordered]@{ eliteRarities = @('apotheosis:rare', 'apotheosis:epic');      affixChance = 0.55; rewardRarities = @('apotheosis:rare', 'apotheosis:epic');      purities = @('flawless');               dust = 16; heraldXp = 200; bossXp = 500; finalXp = 800;  stackCount = 12 }
    summit   = [pscustomobject][ordered]@{ eliteRarities = @('apotheosis:epic', 'apotheosis:mythic');    affixChance = 0.65; rewardRarities = @('apotheosis:epic', 'apotheosis:mythic');    purities = @('flawless', 'perfect');    dust = 20; heraldXp = 250; bossXp = 650; finalXp = 950;  stackCount = 16 }
    pinnacle = [pscustomobject][ordered]@{ eliteRarities = @('apotheosis:epic', 'apotheosis:mythic');    affixChance = 0.8;  rewardRarities = @('apotheosis:mythic');                     purities = @('perfect');                dust = 24; heraldXp = 300; bossXp = 800; finalXp = 1200; stackCount = 20 }
}

# Theme identity: gateway boss-bar color + themed reward pool per mob family.
# Gems are ids of THIS mod's gems (tensura_apotheosis:<family>/<name>); stacks
# are vanilla flavor items that read as the mob.
$themeIdentity = @{
    demon       = @{ color = '#6A1B9A'; gems = @('core/shadow', 'core/fury', 'dragonscale/darkness');                    stack = 'minecraft:amethyst_shard' }
    salamander  = @{ color = '#E64A19'; gems = @('dragonscale/flame', 'core/vigor', 'core/fury');                        stack = 'minecraft:blaze_powder' }
    scaled      = @{ color = '#558B2F'; gems = @('dragonscale/flame', 'dragonscale/earth', 'dragonscale/wind', 'core/juggernaut'); stack = 'minecraft:dragon_breath' }
    lizard      = @{ color = '#AED581'; gems = @('core/earthen', 'core/predator', 'core/vigor');                         stack = 'minecraft:turtle_scute' }
    hover       = @{ color = '#4DD0E1'; gems = @('dragonscale/wind', 'core/sprinter', 'core/swift');                     stack = 'minecraft:feather' }
    spider      = @{ color = '#616161'; gems = @('core/shadow', 'core/predator', 'core/executioner');                    stack = 'minecraft:spider_eye' }
    insect      = @{ color = '#F9A825'; gems = @('core/swift', 'core/precision', 'core/predator');                       stack = 'minecraft:honeycomb' }
    beast       = @{ color = '#8D6E63'; gems = @('core/predator', 'core/vigor', 'core/fury', 'core/juggernaut');         stack = 'minecraft:bone' }
    aquatic     = @{ color = '#039BE5'; gems = @('core/swift', 'core/striker', 'dragonscale/storm');                     stack = 'minecraft:prismarine_crystals' }
    armorsaurus = @{ color = '#546E7A'; gems = @('core/barrier', 'core/stalwart', 'spirit/guardian');                    stack = 'minecraft:iron_ingot' }
    slime       = @{ color = '#7CB342'; gems = @('core/vigor', 'core/magicule', 'core/mystic');                          stack = 'minecraft:slime_ball' }
    spore       = @{ color = '#AB47BC'; gems = @('core/mystic', 'core/analyst', 'core/magicule');                        stack = 'minecraft:brown_mushroom' }
    orc         = @{ color = '#C62828'; gems = @('core/juggernaut', 'core/brawler', 'core/fury');                         stack = 'minecraft:golden_carrot' }
    unicorn     = @{ color = '#FFD54F'; gems = @('holy/core', 'spirit/core', 'core/vigil', 'holy/judgment');            stack = 'minecraft:gold_ingot' }
}

# Guards: every roster mob must land on a profiled tier and a themed family,
# themed gems must be gems this mod actually emits, and all 5 bands must be
# in use (a band falling out would strand its tier's progression).
$gemRels = [System.Collections.Generic.HashSet[string]]::new()
foreach ($g in $gems) { [void]$gemRels.Add($g.rel) }
$bandsUsed = [System.Collections.Generic.HashSet[string]]::new()
foreach ($r in $roster) {
    [void]$bandsUsed.Add($r.tier)
    if (-not $tierProfile.Contains($r.tier)) { throw "roster $($r.mob): unknown tier '$($r.tier)'" }
    if (-not $themeIdentity.ContainsKey($r.theme)) { throw "roster $($r.mob): no theme identity for '$($r.theme)'" }
}
if ($bandsUsed.Count -ne 5) { throw "gateway tiers in use: $($bandsUsed.Count) expected 5" }
foreach ($t in $themeIdentity.Keys) {
    foreach ($g in $themeIdentity[$t].gems) {
        if (-not $gemRels.Contains($g)) { throw "theme $t references missing gem '$g'" }
    }
}

$weights = [pscustomobject][ordered]@{
    haven     = [pscustomobject]@{ quality = 0.1;  weight = 100 }
    frontier  = [pscustomobject]@{ quality = 0.1;  weight = 100 }
    ascent    = [pscustomobject]@{ quality = 0.1;  weight = 80 }
    summit    = [pscustomobject]@{ quality = 0.12; weight = 60 }
    pinnacle  = [pscustomobject]@{ quality = 0.15; weight = 40 }
}
$validGear = [ordered]@{}
foreach ($t in $tiers) { $validGear[$t] = @() }  # filled per elite

foreach ($r in $roster) {
    if (-not $spawnTags.Contains($r.mob)) {
        throw "no #tensura:$($r.mob)_spawn biome tag - cannot derive spawn biomes"
    }
    $th = $themeByKey[$r.theme]
    $band = $r.tier
    $profile = $tierProfile[$band]
    # Full gating: this elite only exists at world tiers at/above its band -
    # spawn weights and gear pools become a contiguous suffix of the tier
    # ladder (omitted tiers weight zero, craig.json-style).
    $vg = [ordered]@{}
    $wb = [ordered]@{}
    foreach ($t in $tiers) {
        if ($tierIndex[$t] -ge $tierIndex[$band]) {
            $vg[$t] = @("#tensura_$($r.theme)_$t")
            $wb[$t] = $weights.$t
        }
    }

    $attr = [System.Collections.Generic.List[object]]::new()
    $attr.Add([pscustomobject][ordered]@{ attribute='minecraft:generic.max_health'; operation='add_value'; value=(Range $th.hp[0] $th.hp[1]) })
    $attr.Add([pscustomobject][ordered]@{ attribute='minecraft:generic.attack_damage'; operation='add_multiplied_total'; value=(Range $th.dmg[0] $th.dmg[1]) })
    $attr.Add([pscustomobject][ordered]@{ attribute='minecraft:generic.knockback_resistance'; operation='add_value'; value=0.5 })
    $attr.Add([pscustomobject][ordered]@{ attribute='minecraft:generic.scale'; operation='add_value'; value=(Range $th.scale[0] $th.scale[1]) })
    $effects = @($th.effects | ForEach-Object { [pscustomobject][ordered]@{ effect = $_; amplifier = 0 } })

    $stats = [ordered]@{
        attribute_modifiers  = $attr
        effects              = $effects
        enchant_chance       = $th.ench
        enchantment_levels   = [pscustomobject]@{ primary = 20; secondary = 12 }
    }
    $basic = [ordered]@{
        constraints       = [pscustomobject][ordered]@{ biomes = "#tensura:$($r.mob)_spawn" }
        name              = [pscustomobject][ordered]@{ translate = "elite.tensura_apotheosis.$($r.mob)" }
        spawn_conditions  = @([pscustomobject][ordered]@{
            type = 'apotheosis:not'
            spawn_condition = [pscustomobject][ordered]@{
                type = 'apotheosis:spawn_type'
                spawn_types = @('spawn_egg', 'spawner', 'mob_summoned') } })
        valid_gear_sets   = $vg
        weights           = $wb
    }
    $elite = [ordered]@{
        type           = 'apotheosis:elite'
        basic_data     = $basic
        entities       = "tensura:$($r.mob)"
        stats          = $stats
        success_chance = $th.success
        # Elite rarity ladder: the rarities this elite's own gear affixes roll,
        # taken from the shared tier profile - haven-band elites wear
        # common/uncommon affixes, summit/pinnacle elites reach epic/mythic.
        affix_data     = [pscustomobject][ordered]@{
            affix_chance = $profile.affixChance
            rarities     = @($profile.eliteRarities)
        }
    }
    Add-Elite $r.mob $r.mob $r.display ([pscustomobject]$elite)
}

# ======================================================================
# ELITE GATEWAYS (Gateways to Eternity, apotheosis:tiered)
# ======================================================================
# One summonable boss gateway per Tensura elite: a herald wave of the mob's
# lesser kin, then the elite itself via Apotheosis' apotheosis:elite wave
# entity. Each gateway sits at its mob's world tier (canOpen enforces the
# player's unlocked tier), wears the mob family's boss-bar color, and pays
# out of the shared tier profile scaled against a themed gem/stack pool -
# recipes stay uniform: ender pearls, diamonds and a pinch of gem dust.
# NOTE on the herald affix modifier: do NOT add a rarities list here. Its
# AffixData codec resolves rarity values at DECODE time, and Gateways'
# registry reloads before Apotheosis' rarity registry binds (all 38 files
# fail with "unbound value: apotheosis:common"). The omitted field falls
# back to LootRarity.random with default weights - Apotheosis' own
# world-tier rarity weights - which is tier-aware for free. The static band
# only belongs in elite affix_data and the affix_item reward, whose codecs
# hold lazy DynamicHolders.
$gateways  = @()   # @{ rel; json }
$gwRecipes = @()   # @{ rel; gateway }
function Add-Gateway([string]$rel, $json) {
    $script:gateways += @{ rel = $rel; json = $json }
}
function Add-GatewayRecipe([string]$rel, [string]$gateway) {
    $script:gwRecipes += @{ rel = $rel; gateway = $gateway }
}

foreach ($r in $roster) {
    $band = $r.tier
    $profile = $tierProfile[$band]
    $identity = $themeIdentity[$r.theme]
    $heraldWave = [pscustomobject][ordered]@{
        max_wave_time = 2400
        setup_time    = 150
        entities      = @([pscustomobject][ordered]@{
            type      = 'gateways:standard'
            count     = 3
            entity    = "tensura:$($r.mob)"
            modifiers = @(
                [pscustomobject][ordered]@{
                    type      = 'gateways:attribute'
                    attribute = 'minecraft:generic.max_health'
                    operation = 'add_multiplied_total'
                    value     = 0.5
                }
                [pscustomobject][ordered]@{
                    type         = 'apotheosis:affix'
                    affix_chance = 0.5
                })
        })
        rewards       = @([pscustomobject][ordered]@{
            type = 'gateways:experience'; experience = $profile.heraldXp; orb_size = 60 })
    }
    $bossWave = [pscustomobject][ordered]@{
        max_wave_time = 4800
        setup_time    = 200
        entities      = @([pscustomobject][ordered]@{
            type        = 'apotheosis:elite'
            base_entity = [pscustomobject][ordered]@{
                type           = 'gateways:standard'
                entity         = "tensura:$($r.mob)"
                finalize_spawn = $false
                modifiers      = @(
                    [pscustomobject][ordered]@{
                        type      = 'gateways:attribute'
                        attribute = 'minecraft:generic.max_health'
                        operation = 'add_multiplied_total'
                        value     = 1.0
                    }
                    [pscustomobject][ordered]@{
                        type      = 'gateways:attribute'
                        attribute = 'minecraft:generic.attack_damage'
                        operation = 'add_multiplied_total'
                        value     = 0.25
                    })
            }
            desc           = "elite.tensura_apotheosis.$($r.mob)"
            elite          = "tensura_apotheosis:$($r.mob)"
        })
        rewards       = @([pscustomobject][ordered]@{
            type = 'gateways:experience'; experience = $profile.bossXp; orb_size = 100 })
    }
    $gateway = [pscustomobject][ordered]@{
        type     = 'apotheosis:tiered'
        settings = [pscustomobject][ordered]@{
            soundtrack = 'apotheosis:soundtrack_pinnacle'
            tier       = $band
            size       = 'large'
            color      = $identity.color
        }
        waves    = @($heraldWave, $bossWave)
        rewards  = @(
            [pscustomobject][ordered]@{ type = 'apotheosis:affix_item'; rarities = @($profile.rewardRarities) }
            [pscustomobject][ordered]@{
                type     = 'apotheosis:gem'
                purities = @($profile.purities)
                gems     = @($identity.gems | ForEach-Object { "tensura_apotheosis:$_" })
            }
            [pscustomobject][ordered]@{
                type  = 'gateways:stack'
                stack = [pscustomobject][ordered]@{ count = $profile.dust; id = 'apotheosis:gem_dust' }
            }
            [pscustomobject][ordered]@{
                type  = 'gateways:stack'
                stack = [pscustomobject][ordered]@{ count = $profile.stackCount; id = $identity.stack }
            }
            [pscustomobject][ordered]@{ type = 'gateways:experience'; experience = $profile.finalXp; orb_size = 100 }
        )
        rules    = [pscustomobject][ordered]@{
            leash_range            = 48.0
            lives                  = 3
            requires_nearby_player = $true
            spawn_range            = 24.0
        }
    }
    Add-Gateway "elite/$($r.mob)" $gateway
    Add-GatewayRecipe "elite/$($r.mob)" "tensura_apotheosis:elite/$($r.mob)"
}
if ($gateways.Count -ne 38) { throw "gateways = $($gateways.Count) expected 38" }

# ======================================================================
# EMIT
# ======================================================================
foreach ($gs in $gearsets) {
    WriteJsonFile (Join-Path $data ("gear_sets\" + ($gs.rel -replace '/', '\') + '.json')) $gs.json
}
foreach ($el in $elites) {
    WriteJsonFile (Join-Path $data ("apothic_elites\" + ($el.rel -replace '/', '\') + '.json')) $el.json
}
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
            textures = [pscustomobject]@{ layer0 = "$($g.tex)" }
        })
    $lang["item.apotheosis.gem.tensura_apotheosis:$($g.rel)"] = $g.display
}
foreach ($gw in $gateways) {
    WriteJsonFile (Join-Path $data ("gateways\" + ($gw.rel -replace '/', '\') + '.json')) $gw.json
}
foreach ($rc in $gwRecipes) {
    WriteJsonFile (Join-Path $data ("recipe\gateways\" + ($rc.rel -replace '/', '\') + '.json')) ([pscustomobject][ordered]@{
        'neoforge:conditions' = @([pscustomobject][ordered]@{ type = 'neoforge:mod_loaded'; modid = 'gateways' })
        type                  = 'minecraft:crafting_shaped'
        category              = 'misc'
        group                 = 'tensura_apotheosis'
        key                   = [pscustomobject][ordered]@{
            E = [pscustomobject][ordered]@{ tag = 'c:ender_pearls' }
            D = [pscustomobject][ordered]@{ item = 'minecraft:diamond' }
            G = [pscustomobject][ordered]@{ item = 'apotheosis:gem_dust' }
        }
        pattern               = @('EDE', 'DGD', 'EDE')
        result                = [pscustomobject][ordered]@{
            components = [pscustomobject][ordered]@{ 'gateways:gateway' = $rc.gateway }
            count      = 1
            id         = 'gateways:gate_pearl'
        }
    })
}

New-Item -ItemType Directory -Path (Join-Path $assets 'lang') -Force | Out-Null
[System.IO.File]::WriteAllText((Join-Path $assets 'lang\en_us.json'), ($lang | ConvertTo-Json -Depth 5), $utf8)

"affixes written:   $($affixes.Count)"
"gems written:      $($gems.Count)"
"gear sets written: $($gearsets.Count)"
"elites written:    $($elites.Count)"
"gateways written:  $($gateways.Count)"
"gateway recipes:   $($gwRecipes.Count)"
"lang keys:         $($lang.Count)"
