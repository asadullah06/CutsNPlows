package Com.app.cuts.plows.ui.Dashboard

import Com.app.cuts.plows.R
import Com.app.cuts.plows.utils.UserPreferences
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class JobHistoryListAdapter(
    val context: Context,
    var jobsList: ArrayList<JobHistoryListModel>
) : RecyclerView.Adapter<JobHistoryListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.jobs_history_listrow, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binditem(jobsList[position])
    }

    override fun getItemCount() = jobsList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun binditem(jobHistoryListModel: JobHistoryListModel) {
            val nameField = itemView.findViewById<TextView>(R.id.providerNameTextView)
            val roleField = itemView.findViewById<TextView>(R.id.providerLabelTextView)
            val dateField = itemView.findViewById<TextView>(R.id.dateTextView)
            val budgetField = itemView.findViewById<TextView>(R.id.budgetTextView)
            val jobStatusField = itemView.findViewById<TextView>(R.id.statusTextView)
            nameField.text = jobHistoryListModel.userName
            dateField.text = jobHistoryListModel.jobFinishedDate
            budgetField.text = jobHistoryListModel.jobAmount
            jobStatusField.text = jobHistoryListModel.jobStatus


            if (UserPreferences.getClassInstance(context).getUserRole() == context.resources.getString(R.string.customer)) {
                roleField.text = context.resources.getString(R.string.service_provider)
            } else if (UserPreferences.getClassInstance(context).getUserRole() == context.resources.getString(R.string.service_provider)) {
                roleField.text = context.resources.getString(R.string.customer)
            }
        }
    }
}