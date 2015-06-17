package net.sf.colossus.guiutil;


import java.awt.Rectangle;


/**
 * Describes the contract between Non rectangular Jcomponents and Swing layout managers that are aware of non-recticle JComponents.
 * 
 */
public interface INonRecticleJComponent
{
    /**
     * @return The largest Rectangle contained by the non-rectangular component.
     */
    public abstract Rectangle getBaseRectangle();

    /**
     * resizes the base rectangle -- this triggers the non-rectangleJComponent to change it's 
     * preferred size & position to contain the new Rectangle.
     */
    public abstract void resizeBaseRectangle(Rectangle newBaseRectangle);
}
