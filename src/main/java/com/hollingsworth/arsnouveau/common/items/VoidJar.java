package com.hollingsworth.arsnouveau.common.items;

import com.hollingsworth.arsnouveau.api.item.IScribeable;
import com.hollingsworth.arsnouveau.common.capability.ManaCapability;
import com.hollingsworth.arsnouveau.common.lib.LibItemNames;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.ItemsRegistry;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.hollingsworth.arsnouveau.common.items.ItemScroll.ITEM_PREFIX;
import static com.hollingsworth.arsnouveau.common.items.ItemScroll.containsItem;

public class VoidJar extends ModItem implements IScribeable {

    public VoidJar() {
        super(ItemsRegistry.defaultItemProperties().stacksTo(1), LibItemNames.VOID_JAR);
    }

    public void toggleStatus(PlayerEntity playerEntity, ItemStack stack){
        CompoundNBT tag = stack.getTag();
        if(tag.getBoolean("on")){
            tag.putBoolean("on", false);
            PortUtil.sendMessage(playerEntity, new TranslationTextComponent("ars_nouveau.off"));
        }else{
            tag.putBoolean("on", true);
            PortUtil.sendMessage(playerEntity, new TranslationTextComponent("ars_nouveau.on"));
        }
    }

    public static boolean tryVoiding(PlayerEntity player, ItemStack pickingUp) {
        NonNullList<ItemStack> list =  player.inventory.items;
        boolean voided = false;
        for(int i = 0; i < 9; i++){
            ItemStack jar = list.get(i);
            if(jar.getItem() == ItemsRegistry.VOID_JAR){
                if(isActive(jar) && containsItem(pickingUp, jar.getTag())){
                    ManaCapability.getMana(player).ifPresent(iMana -> iMana.addMana(5.0 * pickingUp.getCount()));
                    pickingUp.setCount(0);
                    voided = true;
                    break;
                }
            }
        }
        return voided;
    }

    public static boolean isActive(ItemStack stack){
        return stack.hasTag() && stack.getTag().getBoolean("on");
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity player, Hand handIn) {
        if(worldIn.isClientSide)
            return super.use(worldIn, player, handIn);
        ItemStack stack = player.getItemInHand(handIn);
        CompoundNBT tag = stack.getOrCreateTag();

        if(handIn == Hand.MAIN_HAND){
            ItemStack stackToWrite = player.getOffhandItem();
            if(player.isShiftKeyDown()){
                toggleStatus(player, stack);
                return ActionResult.consume(stack);
            }

            if(!stackToWrite.isEmpty()){
                if(containsItem(stackToWrite, tag)) {
                    PortUtil.sendMessage(player, new TranslationTextComponent("ars_nouveau.scribe.item_removed"));
                    ItemScroll.removeItem(stackToWrite, tag);
                    player.startUsingItem(handIn);
                    return ActionResult.fail(stack);
                }
                PortUtil.sendMessage(player, new TranslationTextComponent("ars_nouveau.scribe.item_added"));
                ItemScroll.addItem(stackToWrite, tag);
                player.startUsingItem(handIn);
                return ActionResult.fail(stack);
            }

        }

        return ActionResult.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip2, ITooltipFlag flagIn) {
        CompoundNBT tag = stack.getTag();
        if(tag == null)
            return;

        if(tag.getBoolean("on")){
            tooltip2.add(new TranslationTextComponent("ars_nouveau.on"));
        }else{
            tooltip2.add(new TranslationTextComponent("ars_nouveau.off"));
        }


        super.appendHoverText(stack, worldIn, tooltip2, flagIn);
        List<ItemStack> stacks = new ArrayList<>();
        for(String s : tag.getAllKeys()){
            if(s.contains(ITEM_PREFIX)){
                stacks.add(ItemStack.of(tag.getCompound(s)));
            }
        }
        for(ItemStack s : stacks){
            tooltip2.add(s.getHoverName());
        }
    }

    @Override
    public boolean onScribe(World world, BlockPos pos, PlayerEntity player, Hand handIn, ItemStack thisStack) {
        return ItemScroll.scribe(world, pos, player, handIn, thisStack);
    }
}
