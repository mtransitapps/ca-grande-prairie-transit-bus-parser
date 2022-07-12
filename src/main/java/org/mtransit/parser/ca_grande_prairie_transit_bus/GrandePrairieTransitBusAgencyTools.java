package org.mtransit.parser.ca_grande_prairie_transit_bus;

import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://data.cityofgp.com/Transportation/GP-Transit-GTFS-Feed/kwef-vsek
// https://data.cityofgp.com/download/kwef-vsek/ZIP
// http://jump.nextinsight.com/gtfs/ni_gp/google_transit.zip
// https://gpt.mapstrat.com/current/google_transit.zip
public class GrandePrairieTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new GrandePrairieTransitBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Grande Prairie Transit";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Nullable
	@Override
	public Long convertRouteIdFromShortNameNotSupported(@NotNull String routeShortName) {
		if ("SJP".equalsIgnoreCase(routeShortName)) {
			return 8L;
		} else if ("SJS".equalsIgnoreCase(routeShortName)) {
			return 9L;
		}
		return null;
	}

	private static final Pattern STARTS_WITH_ROUTE_ = Pattern.compile("(^route )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanRouteShortName(@NotNull String routeShortName) {
		routeShortName = STARTS_WITH_ROUTE_.matcher(routeShortName).replaceAll(EMPTY);
		return routeShortName;
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR_GREEN = "056839"; // GREEN (from PNG logo)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		return false; // not useful because all loops
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern ENDS_WITH_P_ = Pattern.compile("( \\(.+\\)$)");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = ENDS_WITH_P_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern DASH_ = Pattern.compile("(^([^-]+)-([^-]+)$)", Pattern.CASE_INSENSITIVE);
	private static final String DASH_REPLACEMENT = "$2 - $3";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = DASH_.matcher(gStopName).replaceAll(DASH_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
