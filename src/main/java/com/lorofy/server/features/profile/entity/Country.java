package com.lorofy.server.features.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.util.UUID;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @Column(name = "code", length = 2)
    @JdbcTypeCode(Types.CHAR)
    private String code;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "flag_asset_id")
    private UUID flagAssetId;
}
