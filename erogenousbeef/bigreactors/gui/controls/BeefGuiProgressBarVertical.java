package erogenousbeef.bigreactors.gui.controls;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.inventory.Container;
import net.minecraft.util.Icon;
import erogenousbeef.bigreactors.client.gui.BeefGuiBase;
import erogenousbeef.bigreactors.common.BigReactors;
import erogenousbeef.bigreactors.gui.BeefGuiControlBase;
import erogenousbeef.bigreactors.gui.IBeefGuiControl;

public abstract class BeefGuiProgressBarVertical extends BeefGuiControlBase implements IBeefGuiControl {

	private final static int controlWidth = 20;
	private final static int controlHeight = 64;
	private final static String controlTexture = "VerticalProgressBar.png";
	
	public BeefGuiProgressBarVertical(BeefGuiBase container, int x, int y) {
		super(container, x, y, controlWidth, controlHeight);
	}
	
	protected abstract Icon getProgressBarIcon();
	
	protected abstract float getProgress();
	
	protected String getTextureSheet() {
		return "/terrain.png";
	}
	
	@Override
	public void drawBackground(int mouseX, int mouseY) {
		// Draw the background
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		Minecraft.getMinecraft().renderEngine.bindTexture(BigReactors.GUI_DIRECTORY + controlTexture);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(this.x, this.y + this.height, 0, 0, 1.0);
		tessellator.addVertexWithUV(this.x + this.width, this.y + this.height, 0, 0.61, 1.0);
		tessellator.addVertexWithUV(this.x + this.width, this.y, 0, 0.61, 0);
		tessellator.addVertexWithUV(this.x, this.y, 0, 0, 0);
		tessellator.draw();
		
		float progress = getProgress();
		// Draw the bar itself, on top of the background
		if(progress > 0.0) {
			int barHeight = Math.max(1, (int)Math.floor(progress * (float)(this.height - 4)));
			
			Icon progressBarIcon = getProgressBarIcon();
			if(progressBarIcon == null) {
				return;
			}

			double minU = progressBarIcon.getMinU();
			double minV = progressBarIcon.getMinV();
			double maxU = progressBarIcon.getMaxU();
			double maxV = progressBarIcon.getMaxV();
			
			// derp?
			Minecraft.getMinecraft().renderEngine.bindTexture(getTextureSheet());
			
			int barMinX = this.x + 2;
			int barMaxX = this.x + this.width - 2;
			int barMinY = this.y + this.height - 2 - barHeight;
			int barMaxY = this.y + this.height - 2;
			
			// Draw the bar in 16-pixel slices from the bottom up.
			for(int slicedBarY = barMaxY; slicedBarY > 0; slicedBarY -= 16) {
				int slicedBarHeight = (int)Math.min(slicedBarY - barMinY, 16.0f);
				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(barMinX, slicedBarY, 1, minU, minV + (maxV - minV) * slicedBarHeight / 16.0f);
				tessellator.addVertexWithUV(barMaxX, slicedBarY, 1, maxU, minV + (maxV - minV) * slicedBarHeight / 16.0f);
				tessellator.addVertexWithUV(barMaxX, slicedBarY - slicedBarHeight, 1, maxU, minV);
				tessellator.addVertexWithUV(barMinX, slicedBarY - slicedBarHeight, 1, minU, minV);
				tessellator.draw();
			}
		}
	}
	
	@Override public void drawForeground(int mouseX, int mouseY) {
		
	}
}
