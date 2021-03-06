package com.yeahmobi.datasystem.query.plugin;

import java.util.Map;

import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

import com.yeahmobi.datasystem.query.extensions.DataSourcePlugin;
import com.yeahmobi.datasystem.query.extensions.DataSourceView;
import com.yeahmobi.datasystem.query.extensions.Dimentions;
import com.yeahmobi.datasystem.query.extensions.InstalledDataSource;
import com.yeahmobi.datasystem.query.extensions.IntervalTables;
import com.yeahmobi.datasystem.query.extensions.Metrics;
import com.yeahmobi.datasystem.query.jersey.MetricDimentionManager;
import com.yeahmobi.datasystem.query.meta.DimensionDetail;
import com.yeahmobi.datasystem.query.meta.DimensionTable;
import com.yeahmobi.datasystem.query.meta.IntervalTable;
import com.yeahmobi.datasystem.query.meta.IntervalUnit;
import com.yeahmobi.datasystem.query.meta.MetricAggTable;
import com.yeahmobi.datasystem.query.meta.MetricDetail;
import com.yeahmobi.datasystem.query.plugin.handler.DetailQueryDatasetHandler;
import com.yeahmobi.datasystem.query.plugin.handler.DetailQueryPreHandler;
import com.yeahmobi.datasystem.query.pretreatment.PretreatmentHandler;
import com.yeahmobi.datasystem.query.pretreatment.ReportContext;
import com.yeahmobi.datasystem.query.skeleton.DataSetHandler;
import com.yeahmobi.datasystem.query.skeleton.PostContext;

/**
 * plugin of yeahmobi read file
 * 
 * @author ellis 2014.9.12
 */
public class YeahMobiPlugin extends Plugin {

    public YeahMobiPlugin(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Extension
    public static class YeahMobiExtention implements InstalledDataSource {

        public DataSourcePlugin getPlugin() {

            final String dataSourceName = "ymds_druid_datasource";
            DataSourceView view = new DataSourceView() {

                @Override
                public Metrics metrics() {
                    Metrics metrics = new Metrics() {

                        @Override
                        public Map<String, MetricDetail> getMap() {
                        	return MetricDimentionManager.getMetric(this.getClass().getClassLoader(), dataSourceName).getTable();
                        }

                        @Override
                        public MetricAggTable getMetricAggTable() {
                        	return MetricDimentionManager.getMetric(this.getClass().getClassLoader(), dataSourceName);
                        }
                    };
                    return metrics;
                }

                @Override
                public Dimentions dimentions() {
                    Dimentions dimentions = new Dimentions() {
                        @Override
                        public Map<String, DimensionDetail> getMap() {
                        	return MetricDimentionManager.getDimension(this.getClass().getClassLoader(), dataSourceName).getTable();
                        }

                        @Override
                        public DimensionTable getDimensionTable() {
                        	return MetricDimentionManager.getDimension(this.getClass().getClassLoader(), dataSourceName);
                        }
                    };
                    return dimentions;
                }

                @Override
                public IntervalTables intervalTables() {
                    IntervalTables intervalTables = new IntervalTables() {

                        @Override
                        public Map<String, IntervalUnit> getMap() {
                            return IntervalTable.getTable();
                        }
                    };
                    return intervalTables;
                }

                @Override
                public PretreatmentHandler getPreExtraHandler(ReportContext reportContext) {
                    // TODO Auto-generated method stub
                    return new DetailQueryPreHandler();
                }

                @Override
                public DataSetHandler getExtraHandler(PostContext reportFeatures) {
                    // TODO Auto-generated method stub
                    return new DetailQueryDatasetHandler(reportFeatures);
                }

            };

            return new DataSourcePlugin(dataSourceName, view);
        }

    }

}
