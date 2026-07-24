package com.asmontec.bydavm.car.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Redémarre le service de l'agent après le démarrage de l'unité Android
 * ou après une mise à jour de l'application. Ne réagit à aucun autre
 * broadcast : ces deux actions sont protégées par le système et ne
 * peuvent pas être émises par une application tierce.
 */
public final class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case "android.intent.action.QUICKBOOT_POWERON":
                CarAgentService.start(context);
                break;
            default:
                break;
        }
    }
}
