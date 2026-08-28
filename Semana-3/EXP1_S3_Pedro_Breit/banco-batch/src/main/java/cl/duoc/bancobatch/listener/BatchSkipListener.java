package cl.duoc.bancobatch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class BatchSkipListener
        implements SkipListener<Object, Object> {

    private static final Logger logger =
            LoggerFactory.getLogger(BatchSkipListener.class);

    @Override
    public void onSkipInProcess(
            Object item,
            Throwable throwable) {

        logger.warn(
                "REGISTRO OMITIDO | etapa=PROCESS | registro={} | motivo={}",
                item,
                throwable.getMessage()
        );
    }
}
