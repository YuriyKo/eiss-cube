import React from 'react';
import {
	List,
	Filter,
	FunctionField,
	Datagrid,
	ReferenceField,
	TextField,
	ShowButton,
	SelectInput,
	ReferenceInput,
	AutocompleteInput,
	Show,
	SimpleShowLayout,
} from 'react-admin';
import { withStyles } from '@material-ui/core/styles';
import { isSuperAdmin } from '../App';

import Icon from '@material-ui/icons/BarChart';

import PowerChart from './PowerChart';

export const ReportIcon = Icon;

const styles = theme => ({
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    }
});

const types = [
    { id: 'p', name: 'Pulses' },
    { id: 'c', name: 'Cycles' }
];

const ReportListTitle = ({title, record}) => (
	<>
		{title} { record && record.cubeName && `${record.cubeName}` }
	</>
);

const ReportListFilter = props => (
	<Filter {...props}>
		{isSuperAdmin(props.permissions) ? 
            <ReferenceInput
                source='group_id'
                reference='grps'
                sort={{ field: 'displayName', order: 'ASC' }}
                allowEmpty
            >
                <AutocompleteInput optionText='displayName' />
            </ReferenceInput>
        : null }
		<SelectInput label='Report type' source='type' margin='dense' choices={types} />
	</Filter>
);

export const ReportList = withStyles(styles)(
    ({ classes, permissions: p, ...props }) => (
		<List {...props}
			title={<ReportListTitle title='Reports' />}
			filters={<ReportListFilter permissions={p} />}
			sort={{ field: 'cubeID', order: 'ASC' }}
			perPage={10}
			exporter={false}
			bulkActionButtons={false}
		>
			<Datagrid classes={{ rowEven: classes.rowEven }} >
				<ReferenceField label='EISS™Cube' source='cubeID' reference='cubes' link='show'>
                    <TextField source='name' />
                </ReferenceField>

				<ReferenceField label='LORA™Cube' source='cubeID' reference='lora' link='show'>
					<TextField source='name' />
				</ReferenceField>

				{isSuperAdmin(p) ?
					<ReferenceField source='group_id' label='Group' reference='grps' link={false} allowEmpty={true} >
						<TextField source='displayName' />
					</ReferenceField>
				: 
					null
				}
				<FunctionField label='Report type' sortBy='type'
					render={ rd => types.find(r => r.id === rd.type).name }
				/>
				<ShowButton />
			</Datagrid>
		</List>
    )
);
	

export const ReportShow = withStyles(styles)(
    ({ classes, permissions: p, ...props }) => (
		<Show {...props}
			title={<ReportListTitle title='Report from' />} 
		>
			<SimpleShowLayout>
				<PowerChart {...props} />
			</SimpleShowLayout>
		</Show>
	)
);
