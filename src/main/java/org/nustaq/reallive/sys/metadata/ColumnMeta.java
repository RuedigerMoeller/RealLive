package org.nustaq.reallive.sys.metadata;

import java.io.Serializable;

/**
 * Created by ruedi on 09.07.2014.
 */
public class ColumnMeta implements Serializable{
    String name;
    String javaType;
    String displayName;
    int fieldId;
    String customMeta;
    String description;
    String align;
    String renderStyle;
    String bgColor;
    int order;
    private String displayWidth;
    boolean hidden;
    private String textColor;

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public ColumnMeta() {
    }

    public ColumnMeta(String name, String displayName, int fieldId) {
        this.name = name;
        this.displayName = displayName;
        this.fieldId = fieldId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getFieldId() {
        return fieldId;
    }

    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }

    public String getCustomMeta() {
        return customMeta;
    }

    public void setCustomMeta(String customMeta) {
        this.customMeta = customMeta;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public String getRenderStyle() {
        return renderStyle;
    }

    public void setRenderStyle(String renderStyle) {
        this.renderStyle = renderStyle;
    }

    public void setDisplayWidth(String displayWidth) {
        this.displayWidth = displayWidth;
    }

    public String getDisplayWidth() {
        return displayWidth;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getTextColor() {
        return textColor;
    }
}
