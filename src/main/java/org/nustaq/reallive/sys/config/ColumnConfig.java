package org.nustaq.reallive.sys.config;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by ruedi on 03.08.14.
 */
public class ColumnConfig {

    public String align;
    public String bgColor;
    public Integer colOrder;
    public String description;
    public String displayName;
    public String displayWidth;
    public Boolean hidden;
    public String renderStyle;
    public String textColor;

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

    public Integer getColOrder() {
        return colOrder;
    }

    public void setColOrder(Integer colOrder) {
        this.colOrder = colOrder;
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

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
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
