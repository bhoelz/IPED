package iped.app.ui;

import iped.viewers.api.IQuantifiableFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SliderMenuItem extends JPanel implements MenuElement {
  private JSlider valueSlider;
  private JLabel sliderLabel;
  IQuantifiableFilter filter;
  boolean sliderChanged;

  public SliderMenuItem() {
    sliderLabel = new JLabel("");
    sliderLabel.setVisible(true);
    valueSlider = new JSlider();
    valueSlider.setVisible(true);
    valueSlider.setMinimum(0);
    valueSlider.setMaximum(100);
    this.setLayout(new BorderLayout());
    this.add(sliderLabel, BorderLayout.NORTH);
    this.add(valueSlider, BorderLayout.CENTER);
    setSize(new Dimension(100, 48));
  }

  public boolean hasSliderChanged() {
    return sliderChanged;
  }

  public void setFilter(IQuantifiableFilter o) {
    this.filter = o;
    sliderChanged = false;
    setValue(((IQuantifiableFilter) o).getQuantityValue());
    setText("Value:" + Integer.toString(getValue()));
    addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            ((IQuantifiableFilter) o).setQuantityValue(getValue());
            setText("Value:" + Integer.toString(getValue()));
            sliderChanged = true;
          }
        });
  }

  @Override
  public void processMouseEvent(
      MouseEvent event, MenuElement[] path, MenuSelectionManager manager) {
    // TODO Auto-generated method stub

  }

  @Override
  public void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager) {
    // TODO Auto-generated method stub

  }

  @Override
  public void menuSelectionChanged(boolean isIncluded) {
    // TODO Auto-generated method stub

  }

  @Override
  public MenuElement[] getSubElements() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Component getComponent() {
    return this;
  }

  public void addChangeListener(ChangeListener l) {
    valueSlider.addChangeListener(l);
  }

  public void setValue(int n) {
    valueSlider.setValue(n);
  }

  public void setText(String text) {
    sliderLabel.setText(text);
  }

  public int getValue() {
    return valueSlider.getValue();
  }

  @Override
  public int getHeight() {
    return super.getHeight();
  }
}
