package android.promptchuay;

import android.promptchuay.model.Report;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {
    /* จัดการรายการลิสต์ reports ในหน้าของกู้ภัย
    * */
    private List<Report> reports;
    private OnViewMapClickListener onViewMapClickListener;
    
    public interface OnViewMapClickListener {
        void onViewMapClick(Report report);
    }
    
    public ReportsAdapter(List<Report> reports, OnViewMapClickListener onViewMapClickListener) {
        this.reports = reports;
        this.onViewMapClickListener = onViewMapClickListener;
    }
    
    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reports.get(position);
        
        holder.tvReporterName.setText(report.getName());
        holder.tvTime.setText(String.valueOf(report.getTime()));
        holder.tvStatus.setText(report.getStatus());
        holder.tvLevel.setText(report.getLevel());
        holder.tvType.setText(report.getType());
        
        holder.tvLocation.setText(holder.itemView.getContext().getString(
            R.string.coordinates,
            String.valueOf(report.getLocation().lat),
            String.valueOf(report.getLocation().lng)
        ));
        
        holder.tvContact.setText(holder.itemView.getContext().getString(
            R.string.contact_label,
            report.getContact()
        ));
        
        holder.tvDetails.setText(report.getDetails());
        
        // Set click listener for map button
        holder.btnViewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onViewMapClickListener.onViewMapClick(report);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return reports.size();
    }
    
    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        public TextView tvReporterName;
        public TextView tvTime;
        public TextView tvStatus;
        public TextView tvLocation;
        public TextView tvContact;
        public TextView tvDetails;
        public TextView tvLevel;
        public TextView tvType;
        public MaterialButton btnViewOnMap;
        
        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReporterName = itemView.findViewById(R.id.tvReporterName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            btnViewOnMap = itemView.findViewById(R.id.btnViewOnMap);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }
}