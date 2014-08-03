package org.nustaq.reallive.sys.config;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by ruedi on 03.08.14.
 */
public class ColumnConfig implements Serializable {

    String align;
    String bgColor;
    String colColor;

    public int getColOrder() {
        return colOrder;
    }

    public void setColOrder(int colOrder) {
        this.colOrder = colOrder;
    }

    int colOrder;
    String description;
    String displayName;
    String displayWidth;
    boolean hidden;
    String renderStyle;
    String textColor;

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getColColor() {
        return colColor;
    }

    public void setColColor(String colColor) {
        this.colColor = colColor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(String displayWidth) {
        this.displayWidth = displayWidth;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getRenderStyle() {
        return renderStyle;
    }

    public void setRenderStyle(String renderStyle) {
        this.renderStyle = renderStyle;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }
}
