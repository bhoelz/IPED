package iped.app.timelinegraph;

import org.jfree.chart.LegendItem;
import org.jfree.chart.title.LegendTitle;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

public class IpedLegendItem extends LegendItem {
    IpedCombinedDomainXYPlot plot;

    public IpedLegendItem(String seriesKey, Paint paint, IpedCombinedDomainXYPlot plot) {
        super(seriesKey, paint);
        this.plot = plot;
    }

    @Override
    public Font getLabelFont() {
        if (plot.getIpedChartsPanel().getChartPanel().getExcludedEvents().contains(this.getSeriesKey())) {
            Font f = LegendTitle.DEFAULT_ITEM_FONT;
            Map attributes = f.getAttributes();
            attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            return f.deriveFont(attributes);
        }
        return LegendTitle.DEFAULT_ITEM_FONT;
    }

    @Override
    public Shape getShape() {
        return super.getShape();
    }

}
