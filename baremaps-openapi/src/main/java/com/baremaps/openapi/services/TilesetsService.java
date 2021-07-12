package com.baremaps.openapi.services;

import com.baremaps.api.TilesetsApi;
import com.baremaps.model.TileSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.json.Json;
import org.json.simple.JSONObject;

public class TilesetsService implements TilesetsApi {

  private static final QualifiedType<TileSet> TILESET = QualifiedType.of(TileSet.class).with(Json.class);

  private final Jdbi jdbi;

  @Inject
  public TilesetsService(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public void addTileset(Map<String, Object> requestBody) {
    UUID tilesetId = UUID.randomUUID(); // TODO: Read from body
    jdbi.useHandle(handle -> {
      handle.createUpdate("insert into tilesets (id, tileset) values (:id, CAST(:json AS JSONB))")
          .bind("json", new JSONObject(requestBody).toString())
          .bind("id", tilesetId.toString())
          .execute();
    });
  }

  @Override
  public void deleteTileset(String tilesetId) {
    jdbi.useHandle(handle -> {
      handle.execute("delete from tilesets where id = (?)", tilesetId);
    });
  }

  @Override
  public TileSet getTileset(String tilesetId) {
    TileSet tilesets = jdbi.withHandle(handle ->
        handle.createQuery("select tileset from tilesets where id = :id")
            .bind("id", tilesetId)
            .mapTo(TILESET)
            .one());
    return tilesets;
  }

  @Override
  public List<String> getTilesets() {
    List<String> tilesets = jdbi.withHandle(handle ->
        handle.createQuery("select id from tilesets")
            .mapTo(String.class)
            .list());
    return tilesets;
  }

  @Override
  public void updateTileset(String tilesetId, Map<String, Object> requestBody) {
    jdbi.useHandle(handle -> {
      handle.createUpdate("update tilesets set tileset = cast(:json as jsonb) where id = :id")
          .bind("json", new JSONObject(requestBody).toString())
          .bind("id", tilesetId)
          .execute();
    });
  }
}
