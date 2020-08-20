package com.openfocals.services.network.cloudintercept.integrations;

import android.util.Log;

import com.openfocals.services.network.HTTPHandler;
import com.openfocals.services.network.cloudintercept.CloudIntegrationHandler;
import com.openfocals.services.network.cloudintercept.CloudMockService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TasksIntegration extends CloudIntegrationHandler.CloudIntegration {
    private static final String ICON_URL = "https://bloghubstaffcom.lightningbasecdn.com/wp-content/uploads/2017/10/Todoist-main_logo_positive-300x300.png";
    //private static final String INTEGRATION_ID = "tasks_v1";
    private static final String INTEGRATION_ID = "6b771546-e8d4-4102-9b1d-8ca7f946b49c";

    private static final String INTEGRATION_SPEC = "{\"functions\":{\"get_projects\":{\"url\":\"https://" +
        CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\",\"method\":\"POST\"," +
        "\"body\":{\"actionId\":\"tasks:get_projects\"}},\"get_tasks\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME +
        "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\",\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:get_tasks\"}}," +
        "\"create\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\"," +
        "\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:create\"}},\"check\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME +
        "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\",\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:check\"}}," +
        "\"uncheck\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\"," +
        "\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:uncheck\"}}},\"actions\":[]}";
    private static final String TAG = "FOCALS_TASKS";


    public void register(CloudMockService h) {
        h.getIntegrations().registerIntegration(INTEGRATION_ID, this, "Focals Todoist", "Use todoist on focals", ICON_URL);
    }

    private static String buildIntegrationSpec(String id) {
        return "{\"functions\":{\"get_projects\":{\"url\":\"https://" +
                CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/" + id + "\",\"method\":\"POST\"," +
                "\"body\":{\"actionId\":\"tasks:get_projects\"}},\"get_tasks\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME +
                "/v1/integration/respond/" + id + "\",\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:get_tasks\"}}," +
                "\"create\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/" + id + "\"," +
                "\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:create\"}},\"check\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME +
                "/v1/integration/respond/" + id + "\",\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:check\"}}," +
                "\"uncheck\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/" + id + "\"," +
                "\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:uncheck\"}}},\"actions\":[]}";
    }



    // {"actionId":"tasks:create","projectId":"2058959526","text":"test 3"}
    // {"id":"4064689420","title":"test 3","position":6,"status":"","functions":{"check":{"body":{"projectId":"2058959526","taskId":"4064689420"}}}}

    class Task {
        String projectId;
        String id;
        String title;
        int position = 1;
        String dueDate = null;
        String completeDate = null;
        String updateDate = null;
        boolean is_checked = false;

        JSONObject getJsonObj() {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("title", title);
                o.put("position", position);
                o.put("status", is_checked ? "completed" : "");
                if (dueDate != null) {
                    o.put("dueDate", dueDate);
                }
                if (completeDate != null) {
                    o.put("completeDate", completeDate);
                }
                if (updateDate != null) {
                    o.put("updateDate", updateDate);
                }

                JSONObject funs = new JSONObject();
                JSONObject cfun = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("projectId", projectId);
                body.put("taskId", id);
                cfun.put("body", body);

                if (is_checked) {
                    funs.put("uncheck", cfun);
                } else {
                    funs.put("check", cfun);
                }
                o.put("functions", funs);

                return o;
            } catch (JSONException e) {
                throw new RuntimeException("Json failed");
            }
        }
    };

    class Project {
        String id;
        String title;
        String updateDate = "2019-01-01T00:00:00.000Z";

        HashMap<String, Task> tasks = new HashMap<>();

        public void add(Task t) {
            t.projectId = id;
            tasks.put(t.id, t);
        }

        public JSONArray getTaskListJsonArr() {
            JSONArray a = new JSONArray();
            for (Task t : tasks.values()) {
                a.put(t.getJsonObj());
            }
            return a;
        }

        public JSONObject getJsonObj() {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("title", title);
                o.put("updateDate", updateDate);

                JSONObject funs = new JSONObject();
                JSONObject cfun = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("projectId", id);
                cfun.put("body", body);

                funs.put("create", cfun);
                funs.put("get_tasks", cfun);

                o.put("functions", funs);
                return o;
            } catch (JSONException e) {
                throw new RuntimeException("json fail");
            }
        }
    };

    HashMap<String, Project> projects = new HashMap<>(); 

    int onid = 33333333;

    
    public JSONArray getProjectsJsonArr() { 
        JSONArray a = new JSONArray();
        for (Project p : projects.values()) {
            a.put(p.getJsonObj());
        }
        return a;
    }

    public TasksIntegration() {
        Project p1 = new Project();
        p1.id = "b1111111";
        p1.title = "Test project 1";
        p1.updateDate = "2019-10-13T23:59:59.999Z";

        Task p1_t1 = new Task();
        p1_t1.id = "b1111112";
        p1_t1.title = "test task 1";
        p1_t1.position = 5;
        p1.add(p1_t1);

        Task p1_t2 = new Task();
        p1_t2.id = "b1111113";
        p1_t2.title = "test task 2";
        p1_t2.position = 6;
        p1.add(p1_t2);
        
        projects.put(p1.id, p1);


        Project p2 = new Project();
        p2.id = "b2222222";
        p2.title = "Test project 2";
        p2.updateDate = "2019-10-13T23:59:59.999Z";

        Task p2_t1 = new Task();
        p2_t1.id = "b2222223";
        p2_t1.title = "test task p2 t1";
        p2_t1.position = 1;
        p2.add(p2_t1);

        projects.put(p2.id, p2);
    }


    public CloudIntegrationHandler.IntegrationResponse handleRequest(String method, String id, Map<String, String> params, Map<String, String> headers, JSONObject req) throws Exception {
        String action = req.getString("actionId");

        String response = null;
        Log.i(TAG, "Get task action (" + id + "): " + action + " : " + req.toString());
        if (!id.equals(INTEGRATION_ID)) {
            return new CloudIntegrationHandler.IntegrationResponse(404);
        }

        if (action.equals("tasks:get_spec")) {
            Log.i(TAG, "Sending spec");
            return new CloudIntegrationHandler.IntegrationResponse(buildIntegrationSpec(id));
        } else {
            if (action.equals("tasks:get_projects")) {
                Log.i(TAG, "Sending projects: " + getProjectsJsonArr().toString());
                return new CloudIntegrationHandler.IntegrationResponse(getProjectsJsonArr().toString());
            } else if (action.equals("tasks:get_tasks")) {
                Project p = projects.get(req.getString("projectId"));
                if (p != null) {
                    Log.i(TAG, "Getting task list: " + p.getTaskListJsonArr().toString());
                    return new CloudIntegrationHandler.IntegrationResponse(p.getTaskListJsonArr().toString());
                } else {
                    Log.e(TAG, "Failed to get task list");
                    return new CloudIntegrationHandler.IntegrationResponse("{\"ok\":false,\"name\":\"Error\",\"message\":\"Project not found\"}");
                }

            } else if (action.equals("tasks:create")) {

                Project p = projects.get(req.getString("projectId"));

                if (p != null) {
                    Task tnew = new Task();
                    tnew.title = req.getString("text");
                    tnew.id = "" + onid;
                    onid += 1;

                    p.add(tnew);

                    Log.i(TAG, "Calling create task");
                    return new CloudIntegrationHandler.IntegrationResponse(tnew.getJsonObj().toString());
                } else {
                    Log.e(TAG, "Calling create task but couldn't find project");
                    return new CloudIntegrationHandler.IntegrationResponse("{\"ok\":false,\"name\":\"Error\",\"message\":\"Project not found\"}");
                }


            } else if (action.equals("tasks:check")) {
                Project p = projects.get(req.getString("projectId"));

                if (p != null) {
                    Task t = p.tasks.get(req.getString("taskId"));
                    if (t != null) {
                        t.is_checked = true;
                        Log.i(TAG, "Calling check task");
                        return new CloudIntegrationHandler.IntegrationResponse(t.getJsonObj().toString());
                    }
                }

                Log.e(TAG, "Failed calling check task");
                return new CloudIntegrationHandler.IntegrationResponse(
                        "{\"ok\":false,\"name\":\"Error\",\"message\":\"Project or task not found\"}"
                );

            } else if (action.equals("tasks:uncheck")) {
                Project p = projects.get(req.getString("projectId"));
                if (p != null) {
                    Task t = p.tasks.get(req.getString("taskId"));
                    if (t != null) {
                        t.is_checked = false;
                        Log.i(TAG, "Calling uncheck task");
                        return new CloudIntegrationHandler.IntegrationResponse(t.getJsonObj().toString());
                    }
                }

                Log.e(TAG, "Failed calling uncheck task");
                return new CloudIntegrationHandler.IntegrationResponse(
                        "{\"ok\":false,\"name\":\"Error\",\"message\":\"Project or task not found\"}"
                );

            } else {
                Log.e(TAG, "Unknown action");
                return new CloudIntegrationHandler.IntegrationResponse(
                        "{\"ok\":false,\"name\":\"ForbiddenError\",\"message\":\"Forbidden\"}"
                );
            }
        }
    }




}
