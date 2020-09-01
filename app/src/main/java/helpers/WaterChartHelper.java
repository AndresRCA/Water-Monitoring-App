package helpers;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.anychart.AnyChart;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;
import com.example.watermonitoring.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import models.WaterChartItem;

public class WaterChartHelper {

    private Cartesian cartesian;
    private Set set;
    private Context context;

    public WaterChartHelper(Context app_context, @NotNull ArrayList<WaterChartItem> water_set) {
        context = app_context;
        cartesian = AnyChart.line();

        cartesian.animation(true);

        cartesian.padding(10d, 20d, 5d, 20d);
        cartesian.background().fill("#404040");

        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                // TODO ystroke
                .yStroke((Stroke) null, null, null, (String) null, (String) null);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title(context.getString(R.string.chart_title));

        cartesian.yAxis(0).title(context.getString(R.string.chart_y_title));
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        List<DataEntry> seriesData = new ArrayList<>();

        for (WaterChartItem item : water_set) {
            seriesData.add(new CustomDataEntry(item.sample.getStrDate("dd/MM"), item.sample.pH, item.sample.orp, item.sample.turbidity));
        }

        set = Set.instantiate();
        Log.i("water/WaterChartHelper", "inserting data... " + seriesData.size() + " values");
        set.data(seriesData);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");
        Mapping series2Mapping = set.mapAs("{ x: 'x', value: 'value2' }");
        Mapping series3Mapping = set.mapAs("{ x: 'x', value: 'value3' }");

        Line series1 = cartesian.line(series1Mapping);
        series1.name(context.getString(R.string.chart_series1_name));
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Line series2 = cartesian.line(series2Mapping);
        series2.name(context.getString(R.string.chart_series2_name));
        series2.hovered().markers().enabled(true);
        series2.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series2.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Line series3 = cartesian.line(series3Mapping);
        series3.name(context.getString(R.string.chart_series3_name));
        series3.hovered().markers().enabled(true);
        series3.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series3.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(13d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);
    }

    public Cartesian getCartesian() {
        return cartesian;
    }

    // improve this function...
    /*
     * insert the data to be displayed to the chart.
     * @param series_data
     */
    public void insertSeriesData(ArrayList<WaterChartItem> chart_data) {
        Log.i("water/insertSeriesData", "inserting data... " + chart_data.size() + " values");
        List<DataEntry> seriesData = new ArrayList<>();
        for (WaterChartItem item : chart_data) {
            seriesData.add(new CustomDataEntry(item.sample.getStrDate("dd/MM"), item.sample.pH, item.sample.orp, item.sample.turbidity));
        }
        set.data(seriesData); // this will refresh the chart with the new data
    }

    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2, Number value3) {
            super(x, value);
            setValue("value2", value2);
            setValue("value3", value3);
        }
    }
}