package iped.engine.task;

import iped.configuration.Configurable;
import iped.data.IHashValue;
import iped.data.IItem;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.EnableTaskProperty;
import iped.engine.task.index.IndexItem;
import iped.utils.HashValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Tarefa de verificação de arquivos duplicados. Ignora o arquivo caso
 * configurado.
 *
 */
public class DuplicateTask extends AbstractTask {

    public static String HASH_MAP = "HashTaskHashMap"; //$NON-NLS-1$

    private static final String ENABLE_PARAM = "ignoreDuplicates"; //$NON-NLS-1$

    private static final String TASK_EXECUTED = DuplicateTask.class.getName() + "_EXECUTED";

    private HashMap<IHashValue, IHashValue> hashMap;

    private static boolean ignoreDuplicates = false;

    public static boolean isIgnoreDuplicatesEnabled() {
        return ignoreDuplicates;
    }

    @Override
    public List<Configurable<?>> getConfigurables() {
        return Arrays.asList(new EnableTaskProperty(ENABLE_PARAM));
    }

    public void process(IItem evidence) {

        if (Boolean.valueOf((String) evidence.getTempAttribute(TASK_EXECUTED))) {
            return;
        }

        // Verificação de duplicados
        boolean isDuplicate = false;
        IHashValue hashValue = evidence.getHashValue();
        if (hashValue != null) {
            synchronized (hashMap) {
                if (!hashMap.containsKey(hashValue)) {
                    hashMap.put(hashValue, hashValue);
                } else {
                    isDuplicate = true;
                }

            }
        }

        if (ignoreDuplicates && isDuplicate && !evidence.isDir() && !evidence.isRoot()
                && !caseData.isIpedReport()) {
            evidence.setToIgnore(true);
        }

        evidence.setTempAttribute(TASK_EXECUTED, Boolean.TRUE.toString());
    }

    @Override
    public void init(ConfigurationManager configurationManager) throws Exception {

        ignoreDuplicates = configurationManager.getEnableTaskProperty(ENABLE_PARAM);

        hashMap = (HashMap<IHashValue, IHashValue>) caseData.getCaseObject(HASH_MAP);
        if (hashMap == null) {
            hashMap = new HashMap<IHashValue, IHashValue>();
            caseData.putCaseObject(HASH_MAP, hashMap);

            worker.getIndexingPort().distinctFieldValues(IndexItem.HASH).forEach(hash -> {
                IHashValue hValue = new HashValue(hash);
                hashMap.put(hValue, hValue);
            });
        }

    }

    @Override
    public void finish() throws Exception {
        hashMap.clear();
    }

}
