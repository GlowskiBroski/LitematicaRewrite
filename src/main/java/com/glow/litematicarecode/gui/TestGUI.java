package com.glow.litematicarecode.gui;

import com.glow.gbgui.baselib.math.Vector;
import com.glow.gbgui.gui.BaseGUI;
import com.glow.gbgui.gui.element.GuiButton;

public class TestGUI extends BaseGUI {

    public final GuiButton button = new GuiButton(this,"Button Title",new Vector(100,100),new Vector(200,60),(g) -> {
        System.out.println("CLICKe");
    });

    public TestGUI(String title) {
        super(title);
        addGuiElements(button);
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onClose() {

    }
}
