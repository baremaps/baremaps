package com.baremaps.openapi.services;

import com.baremaps.api.StylesApi;
import com.baremaps.model.Link;
import com.baremaps.model.MbStyle;
import com.baremaps.model.StyleSet;
import com.baremaps.model.StyleSetEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.json.Json;

public class StylesService implements StylesApi {

  private static final QualifiedType<MbStyle> MBSTYLE = QualifiedType.of(MbStyle.class).with(Json.class);

  private final Jdbi jdbi;

  @Inject
  public StylesService(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public void addStyle(MbStyle mbStyle) {
    UUID styleId = UUID.randomUUID(); // TODO: Read from body
    jdbi.useHandle(handle -> {
      handle.createUpdate("insert into styles (id, style) values (:id, :json)")
          .bindByType("json", mbStyle, MBSTYLE)
          .bind("id", styleId.toString())
          .execute();
    });
  }

  @Override
  public void deleteStyle(String styleId) {
    jdbi.useHandle(handle -> {
      handle.execute("delete from styles where id = (?)", styleId);
    });
  }

  @Override
  public MbStyle getStyle(String styleId) {
    MbStyle style = jdbi.withHandle(handle ->
        handle.createQuery("select style from styles where id = :id")
            .bind("id", styleId)
            .mapTo(MBSTYLE)
            .one());
    return style;
  }

  @Override
  public StyleSet getStyleSet() {
    List<String> ids = jdbi.withHandle(handle ->
        handle.createQuery("select id from styles")
            .mapTo(String.class)
            .list());

    StyleSet styleSet = new StyleSet();
    List<StyleSetEntry> entries = new ArrayList<StyleSetEntry>();

    for (String id : ids) {
      Link link = new Link();
      link.setHref(
          "http://localhost:8080/styles/" + id); // TODO: set dynamically from server or where the server gets it from
      link.setType("application/vnd.mapbox.style+json");
      link.setRel("stylesheet");

      List<Link> links = new ArrayList<>();
      links.add(link);

      StyleSetEntry entry = new StyleSetEntry();
      entry.setId(id);
      entry.setLinks(links);

      entries.add(entry);
    }

    styleSet.setStyles(entries);

    return styleSet;
  }

  @Override
  public void updateStyle(String styleId, MbStyle mbStyle) {
    jdbi.useHandle(handle -> {
      handle.createUpdate("update styles set style = :json where id = :id")
          .bindByType("json", mbStyle, MBSTYLE)
          .bind("id", styleId)
          .execute();
    });
  }
}
