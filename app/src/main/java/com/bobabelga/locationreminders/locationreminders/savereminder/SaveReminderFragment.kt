package com.bobabelga.locationreminders.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.bobabelga.locationreminders.R
import com.bobabelga.locationreminders.base.BaseFragment
import com.bobabelga.locationreminders.base.NavigationCommand
import com.bobabelga.locationreminders.databinding.FragmentSaveReminderBinding
import com.bobabelga.locationreminders.locationreminders.geofence.GeofenceBroadcastReceiver
import com.bobabelga.locationreminders.locationreminders.reminderslist.ReminderDataItem
import com.bobabelga.locationreminders.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

//            DONE : use the user entered reminder details to:
//             1) add a geofencing request

            val reminderData = ReminderDataItem(title,description,location,latitude,longitude)
            addGeofence(reminderData)

//             2) save the reminder to the local db

            _viewModel.validateAndSaveReminder(reminderData)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
    @SuppressLint("MissingPermission")
    private fun addGeofence(reminderData: ReminderDataItem) {

        val geofence = Geofence.Builder()
                .setRequestId(reminderData.id)
                .setCircularRegion(reminderData.latitude!!,
                        reminderData.longitude!!,
                        10000f
                )
                .setExpirationDuration(1000 * 60 * 5)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

        val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.w(TAG, "Add Geofence " + geofence.requestId)
            }
            addOnFailureListener { e ->
                val errorMessage: String = getErrorString(e)
                Log.w(TAG, "Geofence Not Added " + errorMessage)
            }
        }

    }
    fun getErrorString(e: Exception): String {
        if (e is ApiException) {
            when (e.statusCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> return getString(
                        R.string.geofence_not_available)
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> return getString(
                        R.string.geofence_too_many_geofences)
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> return getString(
                        R.string.geofence_too_many_pending_intents)
            }
        }
        return e.localizedMessage
    }
}
const val TAG = "SaveReminderFragment"