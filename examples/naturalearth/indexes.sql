-- Licensed under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_antarctic_claim_limit_lines_gix ON
    ne_10m_admin_0_antarctic_claim_limit_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_antarctic_claims_gix ON
    ne_10m_admin_0_antarctic_claims
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_boundary_lines_disputed_areas_gix ON
    ne_10m_admin_0_boundary_lines_disputed_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_boundary_lines_land_gix ON
    ne_10m_admin_0_boundary_lines_land
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_boundary_lines_map_units_gix ON
    ne_10m_admin_0_boundary_lines_map_units
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_boundary_lines_maritime_indicator_gix ON
    ne_10m_admin_0_boundary_lines_maritime_indicator
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_countries_gix ON
    ne_10m_admin_0_countries
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_countries_lakes_gix ON
    ne_10m_admin_0_countries_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_disputed_areas_gix ON
    ne_10m_admin_0_disputed_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_disputed_areas_scale_rank_minor_islands_gix ON
    ne_10m_admin_0_disputed_areas_scale_rank_minor_islands
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_label_points_gix ON
    ne_10m_admin_0_label_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_map_subunits_gix ON
    ne_10m_admin_0_map_subunits
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_map_units_gix ON
    ne_10m_admin_0_map_units
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_pacific_groupings_gix ON
    ne_10m_admin_0_pacific_groupings
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_scale_rank_gix ON
    ne_10m_admin_0_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_scale_rank_minor_islands_gix ON
    ne_10m_admin_0_scale_rank_minor_islands
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_seams_gix ON
    ne_10m_admin_0_seams
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_0_sovereignty_gix ON
    ne_10m_admin_0_sovereignty
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_label_points_details_gix ON
    ne_10m_admin_1_label_points_details
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_label_points_gix ON
    ne_10m_admin_1_label_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_seams_gix ON
    ne_10m_admin_1_seams
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_states_provinces_gix ON
    ne_10m_admin_1_states_provinces
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_states_provinces_lakes_gix ON
    ne_10m_admin_1_states_provinces_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_states_provinces_lines_gix ON
    ne_10m_admin_1_states_provinces_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_states_provinces_scale_rank_gix ON
    ne_10m_admin_1_states_provinces_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_admin_1_states_provinces_scale_rank_minor_islands_gix ON
    ne_10m_admin_1_states_provinces_scale_rank_minor_islands
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_airports_gix ON
    ne_10m_airports
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_antarctic_ice_shelves_lines_gix ON
    ne_10m_antarctic_ice_shelves_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_antarctic_ice_shelves_polys_gix ON
    ne_10m_antarctic_ice_shelves_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_coastline_gix ON
    ne_10m_coastline
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_geographic_lines_gix ON
    ne_10m_geographic_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_geography_marine_polys_gix ON
    ne_10m_geography_marine_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_geography_regions_elevation_points_gix ON
    ne_10m_geography_regions_elevation_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_geography_regions_points_gix ON
    ne_10m_geography_regions_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_geography_regions_polys_gix ON
    ne_10m_geography_regions_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_glaciated_areas_gix ON
    ne_10m_glaciated_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_lakes_europe_gix ON
    ne_10m_lakes_europe
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_lakes_gix ON
    ne_10m_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_lakes_historic_gix ON
    ne_10m_lakes_historic
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_lakes_north_america_gix ON
    ne_10m_lakes_north_america
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_lakes_pluvial_gix ON
    ne_10m_lakes_pluvial
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_land_gix ON
    ne_10m_land
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_land_ocean_label_points_gix ON
    ne_10m_land_ocean_label_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_land_ocean_seams_gix ON
    ne_10m_land_ocean_seams
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_land_scale_rank_gix ON
    ne_10m_land_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_minor_islands_coastline_gix ON
    ne_10m_minor_islands_coastline
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_minor_islands_gix ON
    ne_10m_minor_islands
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_minor_islands_label_points_gix ON
    ne_10m_minor_islands_label_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_ocean_gix ON
    ne_10m_ocean
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_ocean_scale_rank_gix ON
    ne_10m_ocean_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_parks_and_protected_lands_area_gix ON
    ne_10m_parks_and_protected_lands_area
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_parks_and_protected_lands_line_gix ON
    ne_10m_parks_and_protected_lands_line
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_parks_and_protected_lands_point_gix ON
    ne_10m_parks_and_protected_lands_point
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_parks_and_protected_lands_scale_rank_gix ON
    ne_10m_parks_and_protected_lands_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_playas_gix ON
    ne_10m_playas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_populated_places_gix ON
    ne_10m_populated_places
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_populated_places_simple_gix ON
    ne_10m_populated_places_simple
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_ports_gix ON
    ne_10m_ports
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_railroads_gix ON
    ne_10m_railroads
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_railroads_north_america_gix ON
    ne_10m_railroads_north_america
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_reefs_gix ON
    ne_10m_reefs
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_rivers_europe_gix ON
    ne_10m_rivers_europe
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_rivers_lake_centerlines_gix ON
    ne_10m_rivers_lake_centerlines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_rivers_lake_centerlines_scale_rank_gix ON
    ne_10m_rivers_lake_centerlines_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_rivers_north_america_gix ON
    ne_10m_rivers_north_america
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_roads_gix ON
    ne_10m_roads
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_roads_north_america_gix ON
    ne_10m_roads_north_america
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_time_zones_gix ON
    ne_10m_time_zones
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_urban_areas_gix ON
    ne_10m_urban_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_10m_urban_areas_landscan_gix ON
    ne_10m_urban_areas_landscan
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_boundary_lines_land_gix ON
    ne_110m_admin_0_boundary_lines_land
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_countries_gix ON
    ne_110m_admin_0_countries
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_countries_lakes_gix ON
    ne_110m_admin_0_countries_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_map_units_gix ON
    ne_110m_admin_0_map_units
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_pacific_groupings_gix ON
    ne_110m_admin_0_pacific_groupings
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_scale_rank_gix ON
    ne_110m_admin_0_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_sovereignty_gix ON
    ne_110m_admin_0_sovereignty
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_0_tiny_countries_gix ON
    ne_110m_admin_0_tiny_countries
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_1_states_provinces_gix ON
    ne_110m_admin_1_states_provinces
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_1_states_provinces_lakes_gix ON
    ne_110m_admin_1_states_provinces_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_1_states_provinces_lines_gix ON
    ne_110m_admin_1_states_provinces_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_admin_1_states_provinces_scale_rank_gix ON
    ne_110m_admin_1_states_provinces_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_coastline_gix ON
    ne_110m_coastline
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_geographic_lines_gix ON
    ne_110m_geographic_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_geography_marine_polys_gix ON
    ne_110m_geography_marine_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_geography_regions_elevation_points_gix ON
    ne_110m_geography_regions_elevation_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_geography_regions_points_gix ON
    ne_110m_geography_regions_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_geography_regions_polys_gix ON
    ne_110m_geography_regions_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_glaciated_areas_gix ON
    ne_110m_glaciated_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_lakes_gix ON
    ne_110m_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_land_gix ON
    ne_110m_land
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_ocean_gix ON
    ne_110m_ocean
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_populated_places_gix ON
    ne_110m_populated_places
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_populated_places_simple_gix ON
    ne_110m_populated_places_simple
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_110m_rivers_lake_centerlines_gix ON
    ne_110m_rivers_lake_centerlines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_boundary_lines_disputed_areas_gix ON
    ne_50m_admin_0_boundary_lines_disputed_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_boundary_lines_land_gix ON
    ne_50m_admin_0_boundary_lines_land
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_boundary_lines_maritime_indicator_gix ON
    ne_50m_admin_0_boundary_lines_maritime_indicator
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_boundary_map_units_gix ON
    ne_50m_admin_0_boundary_map_units
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_breakaway_disputed_areas_gix ON
    ne_50m_admin_0_breakaway_disputed_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_breakaway_disputed_areas_scale_rank_gix ON
    ne_50m_admin_0_breakaway_disputed_areas_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_countries_gix ON
    ne_50m_admin_0_countries
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_countries_lakes_gix ON
    ne_50m_admin_0_countries_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_map_subunits_gix ON
    ne_50m_admin_0_map_subunits
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_map_units_gix ON
    ne_50m_admin_0_map_units
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_pacific_groupings_gix ON
    ne_50m_admin_0_pacific_groupings
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_scale_rank_gix ON
    ne_50m_admin_0_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_sovereignty_gix ON
    ne_50m_admin_0_sovereignty
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_tiny_countries_gix ON
    ne_50m_admin_0_tiny_countries
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_0_tiny_countries_scale_rank_gix ON
    ne_50m_admin_0_tiny_countries_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_1_states_provinces_gix ON
    ne_50m_admin_1_states_provinces
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_1_states_provinces_lakes_gix ON
    ne_50m_admin_1_states_provinces_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_1_states_provinces_lines_gix ON
    ne_50m_admin_1_states_provinces_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_admin_1_states_provinces_scale_rank_gix ON
    ne_50m_admin_1_states_provinces_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_airports_gix ON
    ne_50m_airports
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_antarctic_ice_shelves_lines_gix ON
    ne_50m_antarctic_ice_shelves_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_antarctic_ice_shelves_polys_gix ON
    ne_50m_antarctic_ice_shelves_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_coastline_gix ON
    ne_50m_coastline
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_geographic_lines_gix ON
    ne_50m_geographic_lines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_geography_marine_polys_gix ON
    ne_50m_geography_marine_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_geography_regions_elevation_points_gix ON
    ne_50m_geography_regions_elevation_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_geography_regions_points_gix ON
    ne_50m_geography_regions_points
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_geography_regions_polys_gix ON
    ne_50m_geography_regions_polys
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_glaciated_areas_gix ON
    ne_50m_glaciated_areas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_lakes_gix ON
    ne_50m_lakes
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_lakes_historic_gix ON
    ne_50m_lakes_historic
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_land_gix ON
    ne_50m_land
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_ocean_gix ON
    ne_50m_ocean
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_playas_gix ON
    ne_50m_playas
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_populated_places_gix ON
    ne_50m_populated_places
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_populated_places_simple_gix ON
    ne_50m_populated_places_simple
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_ports_gix ON
    ne_50m_ports
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_rivers_lake_centerlines_gix ON
    ne_50m_rivers_lake_centerlines
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_rivers_lake_centerlines_scale_rank_gix ON
    ne_50m_rivers_lake_centerlines_scale_rank
        USING SPGIST(geom);

CREATE
    INDEX IF NOT EXISTS ne_50m_urban_areas_gix ON
    ne_50m_urban_areas
        USING SPGIST(geom);
