package iped.parsers.ufed.handler;

import iped.data.IItemReader;
import iped.parsers.chat.PartyStringBuilderFactory;
import iped.parsers.ufed.model.Party;
import iped.properties.ExtraProperties;
import iped.properties.MediaTypes;
import iped.search.IItemSearcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.tika.metadata.Metadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static iped.properties.ExtraProperties.*;

@Slf4j
public class PartyHandler extends BaseModelHandler<Party> {


    private String source;
    private Map<String, IItemReader> cache;

    protected PartyHandler(Party model, String source, IItemReader parentItem, Map<String, IItemReader> cache) {
        super(model, parentItem);
        this.cache = cache;
        this.source = source;
    }

    public PartyHandler(Party model, String source) {
        super(model);
        this.source = source;
    }

    @Override
    protected void fillMetadata(String prefix, Metadata metadata) {
        metadata.add(prefix, getTitle());

        model.getReferencedContact().ifPresentOrElse(ref -> {
            metadata.add(prefix + CONVERSATION_SUFFIX_ID, StringUtils.firstNonBlank(model.getIdentifier(), ref.getUserID()));
            metadata.add(prefix + CONVERSATION_SUFFIX_NAME, StringUtils.firstNonBlank(model.getName(), ref.getName()));
            metadata.add(prefix + CONVERSATION_SUFFIX_PHONE, ref.getPhoneNumber());
            metadata.add(prefix + CONVERSATION_SUFFIX_USERNAME, ref.getUsername());
        }, () -> {
            metadata.add(prefix + CONVERSATION_SUFFIX_ID, model.getIdentifier());
            metadata.add(prefix + CONVERSATION_SUFFIX_NAME, model.getName());
        });
    }

    @Override
    public void doLoadReferences(IItemSearcher searcher) {
        if (model.isSystemMessage() || model.getIdentifier() == null) {
            return;
        }

        String identifier = model.getIdentifier();
        if (cache.containsKey(identifier)) {
            IItemReader ref = cache.get(identifier);
            if (ref != null) {
                model.setReferencedContact(ref);
            }
            return;
        }

        // in some cases we've seen weird identifiers like "________", "iciousContentF__t__�ȁ"��Z,",
        if (!isValidPartyIdentifier(identifier)) {
            cache.put(identifier, null);
            return;
        }

        String query;
        if (model.isPhoneOwner()) {
            query = AccountableHandler.createAccountableQuery(identifier, source, MediaTypes.UFED_USER_ACCOUNT_MIME, model, item, searcher);
        } else {
            query = AccountableHandler.createAccountableQuery(identifier, source, MediaTypes.UFED_CONTACT_MIME, model, item, searcher);
        }
        List<IItemReader> results = searcher.search(query).stream()
                .filter(item -> source.equals(item.getMetadataValue(ExtraProperties.UFED_META_PREFIX + "Source"))) // filter for exact source
                .collect(Collectors.toList());
        if (!results.isEmpty()) {
            if (results.size() > 1) {
                // sort getting by items with more metadata
                Collections.sort(results, (item1, item2) -> {
                    return Integer.compare(((Metadata) item2.getMetadata()).names().length,
                            ((Metadata) item1.getMetadata()).names().length);
                });
                log.warn("Found more than 1 party reference: size=[{}] \t query=[{}] \t items=[{}]", results.size(), query, StringUtils.truncate(results.toString(), 0, 1000));
            }
            IItemReader result = results.get(0);
            cache.put(identifier, result);
            model.setReferencedContact(result);
        } else {
            cache.put(identifier, null);
            log.debug("Party reference was not found: {}", model);
        }
    }

    private boolean isValidPartyIdentifier(String identifier) {

        if (StringUtils.isBlank(identifier) || StringUtils.containsWhitespace(identifier)) {
            return false;
        }

        if (StringUtils.isAlphanumeric(identifier) || EmailValidator.getInstance(true, true).isValid(identifier)) {
            return true;
        }

        String shortIdentifier = StringUtils.replaceChars(identifier, "!@#$%&*()=+-_/\\.,;.? ", null);

        if (StringUtils.isBlank(shortIdentifier)) {
            return false;
        }

        if (StringUtils.isAlphanumeric(shortIdentifier)) {
            return true;
        }

        return false;
    }

    @Override
    public String getTitle() {
        return PartyStringBuilderFactory.getBuilder(source)
                .withParty(model)
                .build();
    }
}

