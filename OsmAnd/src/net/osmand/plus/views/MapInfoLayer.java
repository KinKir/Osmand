package net.osmand.plus.views;


import java.lang.reflect.Field;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.views.mapwidgets.BaseMapWidget;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopTextView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.NextTurnInfoWidget;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory.AlarmWidget;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory.RulerWidget;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class MapInfoLayer extends OsmandMapLayer {

	
	private final MapActivity map;
	private final RouteLayer routeLayer;
	private OsmandMapTileView view;
	
	// groups
	private LinearLayout rightStack;
	private LinearLayout leftStack;
	private ImageButton  expand;
	private static boolean expanded = false;
	private BaseMapWidget lanesControl;
	private AlarmWidget alarmControl;
	private RulerWidget rulerControl;
	private MapWidgetRegistry mapInfoControls;

	private OsmandSettings settings;
	private DrawSettings drawSettings;
	private TopTextView streetNameView;


	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		settings = map.getMyApplication().getSettings();
		this.routeLayer = layer;
	}
	
	public MapWidgetRegistry getMapInfoControls() {
		return mapInfoControls;
	}
	
	public MapActivity getMapActivity() {
		return map;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		mapInfoControls = new MapWidgetRegistry(map.getMyApplication().getSettings());
		
		leftStack = (LinearLayout) map.findViewById(R.id.map_left_widgets_panel);
		rightStack = (LinearLayout) map.findViewById(R.id.map_right_widgets_panel);
		expand = (ImageButton) map.findViewById(R.id.map_collapse_button);
		// update and create controls
		registerAllControls();
		
		recreateControls();
	}
	
	public void registerSideWidget(TextInfoWidget widget, int drawableDark,int drawableLight, 
			int messageId, String key, boolean left, int priorityOrder) {
		MapWidgetRegInfo reg = mapInfoControls.registerSideWidgetInternal(widget, drawableDark, drawableLight, messageId, key, left, priorityOrder);
		updateReg(calculateTextState(), reg);
	}
	
	public void registerAllControls(){
		RouteInfoWidgetsFactory ric = new RouteInfoWidgetsFactory();
		MapInfoWidgetsFactory mic = new MapInfoWidgetsFactory();
		OsmandApplication app = view.getApplication();
		lanesControl = ric.createLanesControl(map, view);
		lanesControl.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		lanesControl.setVisibility(View.GONE);
		
		streetNameView = new MapInfoWidgetsFactory.TopTextView(map.getMyApplication(), map);
		updateStreetName(calculateTextState());
		
		alarmControl = ric.createAlarmInfoControl(app, map);
		alarmControl.setVisibility(false);
		
		rulerControl = ric.createRulerControl(app, map);
		rulerControl.setVisibility(false);
		
		// register left stack
		NextTurnInfoWidget bigInfoControl = ric.createNextInfoControl(map, app, false);
		registerSideWidget(bigInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_turn,"next_turn", true, 5);
		NextTurnInfoWidget smallInfoControl = ric.createNextInfoControl(map, app, true);
		registerSideWidget(smallInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_turn_small, "next_turn_small", true,
				10);
		NextTurnInfoWidget nextNextInfoControl = ric.createNextNextInfoControl(map, app, true);
		registerSideWidget(nextNextInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_next_turn, "next_next_turn",true, 15);
		// right stack
		TextInfoWidget intermediateDist = ric.createIntermediateDistanceControl(map);
		registerSideWidget(intermediateDist, R.drawable.widget_intermediate, R.drawable.widget_intermediate, R.string.map_widget_intermediate_distance, "intermediate_distance", false, 3);
		TextInfoWidget dist = ric.createDistanceControl(map);
		registerSideWidget(dist, R.drawable.widget_target, R.drawable.widget_target, R.string.map_widget_distance, "distance", false, 5);
		TextInfoWidget time = ric.createTimeControl(map);
		registerSideWidget(time, R.drawable.widget_time, R.drawable.widget_time, R.string.map_widget_time, "time", false, 10);
		TextInfoWidget speed = ric.createSpeedControl(map);
		registerSideWidget(speed, R.drawable.widget_speed, R.drawable.widget_speed, R.string.map_widget_speed, "speed", false, 15);
		TextInfoWidget gpsInfo = mic.createGPSInfoControl(map);
		registerSideWidget(gpsInfo, R.drawable.widget_gps_info,  R.drawable.widget_gps_info, R.string.map_widget_gps_info, "gps_info", false, 17);
		TextInfoWidget maxspeed = ric.createMaxSpeedControl(map);
		registerSideWidget(maxspeed, R.drawable.widget_max_speed, R.drawable.widget_max_speed, R.string.map_widget_max_speed, "max_speed", false,  18);
		TextInfoWidget alt = mic.createAltitudeControl(map);
		registerSideWidget(alt, R.drawable.widget_altitude, R.drawable.widget_altitude, R.string.map_widget_altitude, "altitude", false, 20);
		TextInfoWidget plainTime = ric.createPlainTimeControl(map);
		registerSideWidget(plainTime, R.drawable.widget_time_to_distance, R.drawable.widget_time_to_distance, R.string.map_widget_plain_time, "plain_time", false, 25);
	}
	
	

	public void recreateControls() {
		rightStack.removeAllViews();
		mapInfoControls.populateStackControl(rightStack, settings.getApplicationMode(), false, expanded);
		leftStack.removeAllViews();
		mapInfoControls.populateStackControl(leftStack, settings.getApplicationMode(), true, expanded);
		leftStack.requestLayout();
		rightStack.requestLayout();
		expand.setVisibility(mapInfoControls.hasCollapsibles(settings.getApplicationMode())? 
				View.VISIBLE : View.GONE);
		this.expand.setImageResource(expanded ? R.drawable.av_upload :
			R.drawable.av_download);
		expand.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				expanded = !expanded;
				recreateControls();
			}
		});
	}
	
	private static class TextState {
		boolean textBold ;
		int textColor ;
		int textShadowColor ;
		int boxTop;
		int rightRes;
		int leftRes;
		int expand;
		int boxFree;
		int textShadowRadius;
	}


	private int themeId = -1;
	public void updateColorShadowsOfText() {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		int calcThemeId = (transparent ? 4 : 0) | (nightMode ? 2 : 0) | (following ? 1 : 0);
		if (themeId != calcThemeId) {
			themeId = calcThemeId;
			TextState ts = calculateTextState();
//			lanesControl.setBackgroundDrawable(boxFree);
			for (MapWidgetRegInfo reg : mapInfoControls.getLeft()) {
				updateReg(ts, reg);
			}
			for (MapWidgetRegInfo reg : mapInfoControls.getRight()) {
				updateReg(ts, reg);
			}
			updateStreetName(ts);
			rulerControl.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, ts.textShadowRadius);
			this.expand.setBackgroundResource(ts.expand);
			rightStack.invalidate();
			leftStack.invalidate();
		}
	}

	private void updateStreetName(TextState ts) {
		streetNameView.setBackgroundResource(ts.boxTop);
		streetNameView.updateTextColor(ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius);
	}

	private void updateReg(TextState ts, MapWidgetRegInfo reg) {
		View v = reg.widget.getView().findViewById(R.id.widget_bg);
		if(v != null) {
			v.setBackgroundResource(reg.left ? ts.leftRes : ts.rightRes);
			reg.widget.updateTextColor(ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius);
		}
	}

	private TextState calculateTextState() {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		TextState ts = new TextState();
		ts.textBold = following;
		ts.textColor = nightMode ? view.getResources().getColor(R.color.widgettext_night) : Color.BLACK;
		// Night shadowColor always use widgettext_shadow_night, same as widget background color for non-transparent
		// night skin (from box_night_free_simple.9.png)
		ts.textShadowColor = nightMode ? view.getResources().getColor(R.color.widgettext_shadow_night) : Color.WHITE;
		if (!transparent && !nightMode) {
			ts.textShadowColor = Color.TRANSPARENT;
		}
		ts.textShadowRadius = ts.textShadowColor == 0 ? 0 : 8; 
		if (transparent) {
			ts.boxTop = R.drawable.btn_flat_trans;
			ts.rightRes = R.drawable.btn_left_round_trans;
			ts.leftRes = R.drawable.btn_right_round_trans;
			ts.expand = R.drawable.btn_inset_circle_trans;
			ts.boxFree = R.drawable.btn_round_trans;
		} else if (nightMode) {
			ts.boxTop = R.drawable.btn_flat_night;
			ts.rightRes = R.drawable.btn_left_round_night;
			ts.leftRes = R.drawable.btn_right_round_night;
			ts.expand = R.drawable.btn_inset_circle_night;
			ts.boxFree = R.drawable.btn_round_night;
		} else {
			ts.boxTop = R.drawable.btn_flat;
			ts.rightRes = R.drawable.btn_left_round;
			ts.leftRes = R.drawable.btn_right_round;
			ts.expand = R.drawable.btn_inset_circle;
			ts.boxFree = R.drawable.btn_round;
		}
		return ts;
	}
	
	
	

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		this.drawSettings = drawSettings;
		// update data on draw
		updateColorShadowsOfText();
		mapInfoControls.updateInfo(settings.getApplicationMode(), drawSettings, expanded);
		streetNameView.updateInfo(drawSettings);
		alarmControl.updateInfo(drawSettings);
		rulerControl.updateInfo(tileBox, drawSettings);
		// TODO
//		lanesControl.updateInfo(drawSettings);
		
	}
	
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	
	public View getProgressBar() {
		// currently no progress on info layer
		return null;
	}


	public static String getStringPropertyName(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("rendering_attr_" + propertyName + "_name");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return defValue;
	}

	public static String getStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("rendering_attr_" + propertyName + "_description");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return defValue;
	}


	
	public ContextMenuAdapter getViewConfigureMenuAdapter() {
		ContextMenuAdapter cm = new ContextMenuAdapter(view.getContext());
		cm.setDefaultLayoutId(R.layout.drawer_list_item);
		cm.item(R.string.layer_map_appearance).icons(R.drawable.ic_back_drawer_dark, R.drawable.ic_back_drawer_white)
				.listen(new OnContextMenuClick() {

					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						map.getMapActions().onDrawerBack();
						return false;
					}
				}).reg();
		cm.item(R.string.app_modes_choose).layout(R.layout.mode_toggles).reg();
		cm.setChangeAppModeListener(new ConfigureMapMenu.OnClickListener() {
			
			@Override
			public void onClick(boolean allModes) {
				map.getMapActions().prepareOptionsMenu(getViewConfigureMenuAdapter());				
			}
		});
		cm.item(R.string.map_widget_reset) 
				.icons(R.drawable.widget_reset_to_default_dark, R.drawable.widget_reset_to_default_light).listen(new OnContextMenuClick() {
					
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						mapInfoControls.resetToDefault();
						recreateControls();
						adapter.notifyDataSetInvalidated();
						return false;
					}
				}).reg();
		final ApplicationMode mode = settings.getApplicationMode();
		mapInfoControls.addControls(this, cm, mode);
		return cm;
	}

	
	
}
