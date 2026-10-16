package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClothConfigScreen.ListWidget.class)
public abstract class MixinClothConfigListWidget {
	@Shadow(remap = false)
	@Final
	private AbstractConfigScreen screen;

	@WrapMethod(method = "renderList", remap = false)
	private void asyncparticles$clipConfigList(GuiGraphics graphics, int startX, int startY,
	                                          int mouseX, int mouseY, float delta, Operation<Void> original) {
		Screen owner = this.screen;
		if (!(owner.getTitle().getContents() instanceof TranslatableContents title)
			|| !title.getKey().equals("gui.asyncparticles")) {
			original.call(graphics, startX, startY, mouseX, mouseY, delta);
			return;
		}

		DynamicEntryListWidget<?> list = (DynamicEntryListWidget<?>) (Object) this;
		graphics.flush();
		// Vanilla buttons must restore this outer clip when their scrolling-text clip is popped.
		graphics.enableScissor(list.left, list.top, list.right, list.bottom);
		try {
			original.call(graphics, startX, startY, mouseX, mouseY, delta);
		} finally {
			try {
				graphics.flush();
			} finally {
				graphics.disableScissor();
			}
		}
	}
}
