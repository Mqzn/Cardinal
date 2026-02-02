package eg.mqzen.cardinal.storage.mongo.mapping.adapter.punishment;

import eg.mqzen.cardinal.api.punishments.IssuerType;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.PunishableType;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.api.punishments.PunishmentID;
import eg.mqzen.cardinal.api.punishments.PunishmentIssuer;
import eg.mqzen.cardinal.api.punishments.PunishmentType;
import eg.mqzen.cardinal.api.punishments.StandardPunishmentType;
import eg.mqzen.lib.bson.Document;
import eg.mqzen.lib.commands.util.TypeWrap;
import eg.mqzen.lib.util.TimeUtil;
import eg.mqzen.cardinal.punishments.core.StandardPunishment;
import eg.mqzen.cardinal.punishments.core.StandardPunishmentID;
import eg.mqzen.cardinal.punishments.issuer.ConsoleIssuer;
import eg.mqzen.cardinal.punishments.issuer.PunishmentIssuerFactory;
import eg.mqzen.cardinal.punishments.target.PunishmentTargetFactory;
import eg.mqzen.cardinal.storage.mongo.mapping.DeserializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.SerializationContext;
import eg.mqzen.cardinal.storage.mongo.mapping.TypeAdapter;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.DeserializationException;
import eg.mqzen.cardinal.storage.mongo.mapping.exception.SerializationException;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunishmentAdapter implements TypeAdapter<Punishment<?>> {

    // Field names for serialization
    private static final String ID_FIELD = "id";
    private static final String TYPE_FIELD = "type";
    private static final String TARGET_FIELD = "target";
    private static final String ISSUER_FIELD = "issuer";
    private static final String REASON_FIELD = "reason";
    private static final String ISSUED_AT_FIELD = "issuedAt";
    private static final String DURATION_FIELD = "duration";
    private static final String EXPIRES_AT_FIELD = "expiresAt";
    private static final String REVOCATION_INFO_FIELD = "revoke-info";
    private static final String NOTES_FIELD = "notes";

    // Target fields
    private static final String TARGET_TYPE_FIELD = "type";
    private static final String TARGET_UUID_FIELD = "uuid";
    private static final String TARGET_NAME_FIELD = "name";
    private static final String TARGET_IP_FIELD = "ipAddress";
    private static final String TARGET_LAST_SEEN_FIELD = "lastSeen";
    private static final String TARGET_PLAYER_DATA_FIELD = "playerData";

    // Issuer fields
    private static final String ISSUER_TYPE_FIELD = "type";
    private static final String ISSUER_UUID_FIELD = "uuid";
    private static final String ISSUER_NAME_FIELD = "name";

    // Revocation fields
    private static final String REVOKER_UUID_FIELD = "revokerUUID";
    private static final String REVOKER_NAME_FIELD = "revokerName";
    private static final String REVOKED_AT_FIELD = "revokedAt";
    private static final String REVOKE_REASON_FIELD = "revokeReason";


    public PunishmentAdapter() {
    }


    @Override
    public Object serialize(Punishment<?> value, SerializationContext context) throws SerializationException {
        Document document = new Document();

        document.append(ID_FIELD, value.getId().getRepresentation());
        document.append(TYPE_FIELD, value.getType().name());
        document.append(TARGET_FIELD, targetToDoc(value.getTarget()));
        document.append(ISSUER_FIELD, issuerToDoc(value.getIssuer()));

        value.getReason().ifPresent(reason -> document.append(REASON_FIELD, reason));

        document.append(ISSUED_AT_FIELD, value.getIssuedAt().toEpochMilli());
        document.append(DURATION_FIELD, TimeUtil.parse(value.getDuration()));

        if(value.isPermanent()) {
            document.append(EXPIRES_AT_FIELD, null);
        }else {
            document.append(EXPIRES_AT_FIELD, value.getExpiresAt().toEpochMilli());
        }
        document.append(REVOCATION_INFO_FIELD, revocationToDoc(value.getRevocationInfo().orElse(null)));

        document.append(NOTES_FIELD, value.getNotes());

        return document;
    }

    private Document targetToDoc(Punishable<?> punishable) {
        Document targetDoc = new Document();
        targetDoc.append(TARGET_TYPE_FIELD, punishable.getType().name());
        targetDoc.append(TARGET_UUID_FIELD, punishable.getTargetUUID().toString());
        targetDoc.append(TARGET_NAME_FIELD, punishable.getTargetName());

        if (punishable.getLastSeen() != null) {
            targetDoc.append(TARGET_LAST_SEEN_FIELD, punishable.getLastSeen().toEpochMilli());
        }

        switch (punishable.getType()) {
            case PLAYER -> {
                // For PlayerTarget, the target is the UUID
                // No additional fields needed beyond the common ones
            }
            case IP_ADDRESS -> {
                // For IPTarget, store the IP address and optional player data
                targetDoc.append(TARGET_IP_FIELD, punishable.getTarget());

                // Check if this IP target has associated player data
                // We can detect this by checking if the name is different from the IP
                if (!punishable.getTargetName().equals(punishable.getTarget())) {
                    // Has associated player data
                    Document playerData = new Document();
                    playerData.append(TARGET_UUID_FIELD, punishable.getTargetUUID().toString());
                    playerData.append(TARGET_NAME_FIELD, punishable.getTargetName());
                    targetDoc.append(TARGET_PLAYER_DATA_FIELD, playerData);
                }
            }
        }

        return targetDoc;
    }

    private Document issuerToDoc(PunishmentIssuer issuer) {
        Document issuerDoc = new Document();
        issuerDoc.append(ISSUER_TYPE_FIELD, issuer.getType().name());
        issuerDoc.append(ISSUER_NAME_FIELD, issuer.getName());
        if(issuer.isPlayer()) {
            issuerDoc.append(ISSUER_UUID_FIELD, issuer.getUniqueId().toString());
        }
        return issuerDoc;
    }

    private Document revocationToDoc(Punishment.RevocationInfo info) {
        if(info == null) {
            return null;
        }
        Document document = new Document();
        if(info.getRevoker().isPlayer()) {
            document.append(REVOKER_UUID_FIELD, info.getRevoker().getUniqueId().toString());
        }
        return document
                .append(REVOKER_NAME_FIELD, info.getRevoker().getName())
                .append(REVOKED_AT_FIELD, info.getRevokedAt().toEpochMilli())
                .append(REVOKE_REASON_FIELD, info.getReason());
    }

    @Override
    public Punishment<?> deserialize(Object value, TypeWrap<Punishment<?>> targetType, DeserializationContext context) throws DeserializationException {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Document document)) {
            throw new DeserializationException("Expected Document for Punishment deserialization, got: " + value.getClass());
        }

        try {
            // Extract basic fields
            String idStr = document.getString(ID_FIELD);
            if (idStr == null) {
                throw new DeserializationException("Missing punishment ID");
            }
            PunishmentID id = new StandardPunishmentID(idStr);

            PunishmentType type = StandardPunishmentType.valueOf(document.getString(TYPE_FIELD));

            Punishable<?> target = deserializeTarget(document.get(TARGET_FIELD, Document.class));
            PunishmentIssuer issuer = deserializeIssuer(document.get(ISSUER_FIELD, Document.class));

            Optional<String> reason = Optional.ofNullable(document.getString(REASON_FIELD));

            Long issuedAtMillis = document.getLong(ISSUED_AT_FIELD);
            if (issuedAtMillis == null) {
                throw new DeserializationException("Missing issuedAt timestamp");
            }
            Instant issuedAt = Instant.ofEpochMilli(issuedAtMillis);

            String durationStr = document.getString(DURATION_FIELD);
            if (durationStr == null) {
                throw new DeserializationException("Missing duration");
            }

            Duration duration = durationStr.isEmpty() ? Duration.ZERO : TimeUtil.parse(durationStr);

            Instant expiresAt = null;
            Long expiresAtMillis = document.getLong(EXPIRES_AT_FIELD);
            if (expiresAtMillis != null) {
                expiresAt = Instant.ofEpochMilli(expiresAtMillis);
            }

            Optional<Punishment.RevocationInfo> revocationInfo = Optional.empty();
            Document revocationDoc = document.get(REVOCATION_INFO_FIELD, Document.class);
            if (revocationDoc != null) {
                revocationInfo = Optional.of(deserializeRevocation(revocationDoc));
            }

            @SuppressWarnings("unchecked")
            List<String> notes = (List<String>) document.getOrDefault(NOTES_FIELD, List.of());

            // Create punishment using builder pattern or constructor
            // Note: You'll need to adjust this based on your actual Punishment implementation
            return createPunishment(id, type, target, issuer, reason.orElse(null), issuedAt, duration, expiresAt,
                    revocationInfo.orElse(null), notes);

        } catch (Exception e) {
            throw new DeserializationException("Failed to deserialize Punishment", e);
        }
    }

    private Punishable<?> deserializeTarget(Document targetDoc) throws DeserializationException {
        if (targetDoc == null) {
            throw new DeserializationException("Missing target document");
        }

        String typeStr = targetDoc.getString(TARGET_TYPE_FIELD);
        if (typeStr == null) {
            throw new DeserializationException("Missing target type");
        }

        PunishableType type = PunishableType.valueOf(typeStr);
        String uuidStr = targetDoc.getString(TARGET_UUID_FIELD);
        String name = targetDoc.getString(TARGET_NAME_FIELD);

        switch (type) {
            case PLAYER -> {
                if (uuidStr == null || name == null) {
                    throw new DeserializationException("Missing UUID or name for PlayerTarget");
                }
                UUID uuid = UUID.fromString(uuidStr);
                return PunishmentTargetFactory.playerTarget(uuid, name);
            }
            case IP_ADDRESS -> {
                String ipAddress = targetDoc.getString(TARGET_IP_FIELD);
                if (ipAddress == null) {
                    throw new DeserializationException("Missing IP address for IPTarget");
                }

                Document playerData = targetDoc.get(TARGET_PLAYER_DATA_FIELD, Document.class);
                if (playerData != null) {
                    // Has associated player data
                    String playerUuidStr = playerData.getString(TARGET_UUID_FIELD);
                    String playerName = playerData.getString(TARGET_NAME_FIELD);
                    if (playerUuidStr != null && playerName != null) {
                        UUID playerUuid = UUID.fromString(playerUuidStr);
                        return PunishmentTargetFactory.playerIPTarget(playerUuid, playerName, ipAddress);
                    }
                }

                // IP-only target
                return PunishmentTargetFactory.ipTarget(ipAddress);
            }
            default -> throw new DeserializationException("Unsupported target type: " + type);
        }
    }

    private PunishmentIssuer deserializeIssuer(Document issuerDoc) throws DeserializationException {
        if (issuerDoc == null) {
            throw new DeserializationException("Missing issuer document");
        }

        String typeStr = issuerDoc.getString(ISSUER_TYPE_FIELD);
        if (typeStr == null) {
            throw new DeserializationException("Missing issuer type");
        }

        IssuerType type = IssuerType.valueOf(typeStr);

        switch (type) {
            case CONSOLE -> {
                return ConsoleIssuer.get();
            }
            case PLAYER -> {
                String uuidStr = issuerDoc.getString(ISSUER_UUID_FIELD);
                String name = issuerDoc.getString(ISSUER_NAME_FIELD);
                if (uuidStr == null || name == null) {
                    throw new DeserializationException("Missing UUID or name for PlayerIssuer");
                }
                UUID uuid = UUID.fromString(uuidStr);

                // Note: You'll need to create a constructor or factory method for PlayerIssuer
                // that takes UUID and name directly, since we don't have the Player object
                return createPlayerIssuer(uuid, name);
            }
            default -> throw new DeserializationException("Unsupported issuer type: " + type);
        }
    }

    private Punishment.RevocationInfo deserializeRevocation(Document revocationDoc) throws DeserializationException {
        String revokerName = revocationDoc.getString(REVOKER_NAME_FIELD);
        Long revokedAtMillis = revocationDoc.getLong(REVOKED_AT_FIELD);
        String reason = revocationDoc.getString(REVOKE_REASON_FIELD);

        if (revokerName == null || revokedAtMillis == null) {
            throw new DeserializationException("Missing revocation data");
        }

        PunishmentIssuer issuer = revocationDoc.getString(REVOKER_UUID_FIELD) != null ?
                PunishmentIssuerFactory.fromPlayerInfo(UUID.fromString(revocationDoc.getString(REVOKER_UUID_FIELD)), revokerName) :
                PunishmentIssuerFactory.fromConsole();

        Instant revokedAt = Instant.ofEpochMilli(revokedAtMillis);

        // Note: You'll need to adjust this based on your actual RevocationInfo implementation
        return createRevocationInfo(issuer, revokedAt, reason);
    }

    // These methods need to be implemented based on your actual class constructors
    private <T> Punishment<T> createPunishment(
            PunishmentID id, PunishmentType type, Punishable<T> target, PunishmentIssuer issuer,
            @Nullable String reason, Instant issuedAt, Duration duration,
            Instant expiresAt, @Nullable Punishment.RevocationInfo revocationInfo,
            List<String> notes) {
        // Implement based on your Punishment class constructor or builder
        StandardPunishment<T> standardPunishment = new StandardPunishment<>(id, type, target, issuer, reason, issuedAt, duration, expiresAt);
        standardPunishment.setRevokeInfo(revocationInfo);
        standardPunishment.setNotesTo(notes);
        return standardPunishment;
    }

    private PunishmentIssuer createPlayerIssuer(UUID uuid, String name) {
        // You'll need to add a constructor to PlayerIssuer that takes UUID and name
        // or create a factory method
        return PunishmentIssuerFactory.fromPlayerInfo(uuid, name);
    }

    private Punishment.RevocationInfo createRevocationInfo(PunishmentIssuer issuer, Instant revokedAt, String reason) {
        // Implement based on your RevocationInfo class
        return new StandardPunishment.StandardRevocationInfo(issuer, revokedAt, reason);
    }

    @Override
    public boolean canHandle(TypeWrap<?> type) {
        return type.isSubtypeOf(Punishment.class);
    }
}