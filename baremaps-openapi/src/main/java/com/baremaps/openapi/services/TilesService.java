package com.baremaps.openapi.services;

import com.baremaps.api.TilesApi;
import com.baremaps.model.TileSetsDescription;
import java.util.List;

public class TilesService implements TilesApi {


  @Override
  public TileSetsDescription describeTilesCollections() {
    return null;
  }

  @Override
  public String getTileSQL(
      String tileMatrixSetId,
      String tileMatrix,
      Integer tileRow,
      Integer tileCol,
      List<String> geodata,
      String sql) {
    throw new UnsupportedOperationException();
  }


}
