/*
 From Gamepiaynmo: https://github.com/Gamepiaynmo/TC-UHC
 */

package me.fallenbreath.tcuhc.task;

import me.fallenbreath.tcuhc.UhcGameColor;
import me.fallenbreath.tcuhc.UhcGameManager;
import me.fallenbreath.tcuhc.UhcGameTeam;
import me.fallenbreath.tcuhc.UhcPlayerManager;
import me.fallenbreath.tcuhc.task.Task.TaskTimer;
import me.fallenbreath.tcuhc.util.TitleUtil;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtByte;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.Collections;

public class TaskTitleCountDown extends TaskTimer {
	
	private int count;

	public TaskTitleCountDown(int init, int delay, int interval) {
		super(delay, interval);
		count = init;
	}
	
	@Override
	public void onTimer() {
		TitleUtil.sendTitleToAllPlayers(Formatting.GOLD + String.valueOf(--count), null);
		if (count == 0) this.setCanceled();
	}
	
	@Override
	public void onFinish() {
		TitleUtil.sendTitleToAllPlayers("Game Started !", "Enjoy Yourself !");
		UhcGameManager.instance.getUhcPlayerManager().getCombatPlayers().forEach(player -> player.addTask(new TaskFindPlayer(player) {
			@SuppressWarnings("ConstantConditions")
			@Override
			public void onFindPlayer(ServerPlayerEntity player) {
				player.changeGameMode(GameMode.SURVIVAL);
				player.setInvulnerable(false);
				player.clearStatusEffects();
				UhcGameManager.instance.getUhcPlayerManager().resetHealthAndFood(player);
				player.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));  // no free phantom
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 4));  // 10s Resistance V
				if(UhcGameManager.getBattleType() == UhcGameManager.EnumBattleType.ICARUS) {
					ItemStack elytra = new ItemStack(Items.ELYTRA);
					elytra.addEnchantment(Enchantments.MENDING, 1);
					elytra.addEnchantment(Enchantments.BINDING_CURSE, 1);
					player.equipStack(EquipmentSlot.CHEST, elytra);
				} else if(UhcGameManager.getBattleType() == UhcGameManager.EnumBattleType.MARINE) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 1200, 0));
				}

				// revoke all advancements
				player.getServer().getAdvancementLoader().getAdvancements().forEach(advancement -> {
					AdvancementProgress advancementProgress = player.getAdvancementTracker().getProgress(advancement);
					if (advancementProgress.isAnyObtained()) {
						for(String string : advancementProgress.getObtainedCriteria()) {
							player.getAdvancementTracker().revokeCriterion(advancement, string);
						}
					}
				});

				// give invisibility and shiny potion to player for ghost mode
				switch (UhcGameManager.getGameMode()) {
					case BOMBER:
						this.getGamePlayer().addBomberModeEffect();
					case GHOST:
						this.getGamePlayer().addGhostModeEffect();
						ItemStack shinyPotion = new ItemStack(Items.SPLASH_POTION).setCustomName(new LiteralText("Splash Shiny Potion"));
						PotionUtil.setCustomPotionEffects(shinyPotion, Collections.singleton(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0)));
						player.getInventory().insertStack(shinyPotion);
						break;
					case HUNTER:
						if(this.getGamePlayer().getTeam().getTeamColor() == UhcGameColor.RED) {
							ItemStack speedPotion = new ItemStack(Items.SPLASH_POTION).setCustomName(new LiteralText("Splash Speedy Potion"));
							PotionUtil.setCustomPotionEffects(speedPotion, Collections.singleton(new StatusEffectInstance(StatusEffects.SPEED, 200, 0)));
							player.getInventory().insertStack(speedPotion);
						} else {
							ItemStack compass = new ItemStack((Items.COMPASS));
							compass.setCustomName(Text.of("Hunter's Compass"));
							compass.addEnchantment(Enchantments.VANISHING_CURSE, 1);
							player.getInventory().insertStack(compass);
						}
						break;
					case GHOSTHUNTER:
						if(this.getGamePlayer().getTeam().getTeamColor() == UhcGameColor.RED) {
							this.getGamePlayer().addGhostModeEffect();
						} else {
							ItemStack shinyPotion2 = new ItemStack(Items.SPLASH_POTION).setCustomName(new LiteralText("Splash Shiny Potion"));
							PotionUtil.setCustomPotionEffects(shinyPotion2, Collections.singleton(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0)));
							player.getInventory().insertStack(shinyPotion2);
							ItemStack compass = new ItemStack((Items.COMPASS));
							compass.setCustomName(Text.of("Hunter's Compass"));
							compass.addEnchantment(Enchantments.VANISHING_CURSE, 1);
							player.getInventory().insertStack(compass);
						}
					case KING:
						if (this.getGamePlayer().isKing()) {
							DyeColor dyeColor = this.getGamePlayer().getTeam().getTeamColor().dyeColor;
							ItemStack kingsHelmet = new ItemStack(Items.LEATHER_HELMET).setCustomName(new LiteralText(String.format("%s crown", dyeColor.getName())));
							kingsHelmet.getOrCreateNbt().put("KingsCrown", NbtByte.of((byte)1));
							kingsHelmet.getOrCreateNbt().put("Unbreakable", NbtByte.of((byte)1));
							((DyeableItem)Items.LEATHER_HELMET).setColor(kingsHelmet, dyeColor.getMapColor().color);
							kingsHelmet.addEnchantment(Enchantments.PROTECTION, 6);
							kingsHelmet.addEnchantment(Enchantments.BINDING_CURSE, 1);
							kingsHelmet.addEnchantment(Enchantments.VANISHING_CURSE, 1);
							player.equipStack(EquipmentSlot.HEAD, kingsHelmet);
						}
						break;
				}
			}
		}));
		if (UhcGameManager.getGameMode() == UhcGameManager.EnumMode.KING) {
			UhcGameManager.instance.addTask(new TaskKingEffectField());
		}
		UhcGameManager.instance.addTask(new TaskScoreboard());
	}

}
