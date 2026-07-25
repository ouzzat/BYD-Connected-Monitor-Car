package ma.asmontec.bydmonitor.mobile.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ma.asmontec.bydmonitor.mobile.security.SecureCredentialStore;

/** Relance la surveillance en arrière-plan après un redémarrage, si un compte est déjà configuré. */
public final class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (new SecureCredentialStore(context).hasCredentials()) {
            MqttSubscriberService.start(context);
        }
    }
}
